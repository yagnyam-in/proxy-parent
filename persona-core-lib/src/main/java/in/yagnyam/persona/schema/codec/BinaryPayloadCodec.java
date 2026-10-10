package in.yagnyam.persona.schema.codec;

import in.yagnyam.persona.schema.ArrayType;
import in.yagnyam.persona.schema.CanonicalJson;
import in.yagnyam.persona.schema.EnumType;
import in.yagnyam.persona.schema.EnvelopeType;
import in.yagnyam.persona.schema.Field;
import in.yagnyam.persona.schema.MapType;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.PrimitiveType;
import in.yagnyam.persona.schema.RecordType;
import in.yagnyam.persona.schema.TypeNode;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The Avro-style binary payload encoding of {@code docs/SCHEMA.md} §3 ({@code payloadFormat =
 * "persona-binary"}).
 *
 * <p>The encoding is schema-driven: it carries no field names and no tags, so a decoder needs the
 * schema (resolved through the envelope's {@code schemaId}) to decode. §3.1 primitives are zigzag
 * LEB128 varints for {@code int}/{@code long}, one byte for booleans, 8 little-endian bytes for
 * doubles, length-prefixed UTF-8 for strings and raw for bytes, and epoch milliseconds for
 * {@code timestamp-millis}. §3.2 compounds concatenate record fields in declaration order, encode
 * enums as their zero-based symbol index, and encode arrays and maps as blocks terminated by a
 * zero count — writers emit exactly one block for a non-empty array or map (map keys in ascending
 * UTF-8 byte order) and readers loop over blocks.
 *
 * <p>{@link EnvelopeType} fields are encoded as records per the built-in schemas of §5. Decoding
 * is strict per §3.3: trailing bytes, truncated input, an enum index outside the symbols, a
 * negative block count and invalid UTF-8 are all rejected.
 *
 * <p>The codec is stateless and thread-safe.
 *
 * <p>Implementation note (not a wire rule): a single block may not declare more than
 * {@value #MAX_BLOCK_ELEMENTS} elements. The limit exists so that a hostile payload can not force
 * unbounded allocation from a handful of bytes; it is far above any legitimate payload size and
 * never affects the bytes written.
 */
public final class BinaryPayloadCodec implements PayloadCodec {

  /** The {@code payloadFormat} identifier of this codec (SCHEMA.md §3). */
  public static final String FORMAT = "persona-binary";

  /** Upper bound on the element count of a single array/map block, a resource-exhaustion guard. */
  private static final int MAX_BLOCK_ELEMENTS = 1 << 20;

  @Override
  public String format() {
    return FORMAT;
  }

  @Override
  public byte[] encode(PersonaSchema schema, Map<String, Object> value) {
    Objects.requireNonNull(schema, "schema");
    Objects.requireNonNull(value, "value");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writeRecord(out, schema.rootRecord(), value, "");
    return out.toByteArray();
  }

  @Override
  public Map<String, Object> decode(PersonaSchema schema, byte[] bytes) {
    Objects.requireNonNull(schema, "schema");
    Objects.requireNonNull(bytes, "bytes");
    Reader reader = new Reader(bytes);
    Map<String, Object> value = readRecord(reader, schema.rootRecord(), "");
    if (reader.remaining() != 0) {
      throw new PayloadDecodingException(
          reader.remaining()
              + " trailing byte(s) after the root record (SCHEMA.md §3.3); the payload is not"
              + " canonically encoded for this schema");
    }
    return value;
  }

  /** Encodes a record as its fields in declaration order (§3.2). */
  private static void writeRecord(
      ByteArrayOutputStream out, RecordType record, Map<String, Object> value, String path) {
    PayloadValues.requireKnownFields(record, value, path);
    for (Field field : record.fields()) {
      Object fieldValue = PayloadValues.requireField(record, value, field, path);
      writeValue(out, field.type(), fieldValue, PayloadValues.childPath(path, field.name()));
    }
  }

  /** Encodes a value of the given type (§3.1, §3.2). */
  private static void writeValue(
      ByteArrayOutputStream out, TypeNode type, Object value, String path) {
    switch (type) {
      case PrimitiveType primitive -> writePrimitive(out, primitive, value, path);
      case EnumType enumType -> writeEnum(out, enumType, value, path);
      case ArrayType arrayType -> writeArray(out, arrayType, value, path);
      case MapType mapType -> writeMap(out, mapType, value, path);
      case RecordType recordType ->
          writeRecord(out, recordType, PayloadValues.requireMap(value, path), path);
      case EnvelopeType envelopeType ->
          writeRecord(out, envelopeType.record(), PayloadValues.requireMap(value, path), path);
    }
  }

  /** Encodes a primitive (§3.1). */
  private static void writePrimitive(
      ByteArrayOutputStream out, PrimitiveType primitive, Object value, String path) {
    switch (primitive.name()) {
      case PrimitiveType.STRING ->
          writeLengthPrefixed(out, CanonicalJson.utf8Bytes(PayloadValues.requireText(value, path)));
      case PrimitiveType.BOOLEAN -> out.write(PayloadValues.requireBoolean(value, path) ? 1 : 0);
      case PrimitiveType.INT -> writeZigzagInt(out, PayloadValues.requireInt(value, path));
      case PrimitiveType.LONG -> writeZigzagLong(out, PayloadValues.requireLong(value, path));
      case PrimitiveType.DOUBLE -> writeDouble(out, PayloadValues.requireDouble(value, path));
      case PrimitiveType.BYTES -> writeLengthPrefixed(out, PayloadValues.requireBytes(value, path));
      case PrimitiveType.TIMESTAMP_MILLIS ->
          writeZigzagLong(out, epochMillis(PayloadValues.requireInstant(value, path), path));
      default -> throw new IllegalStateException("unknown primitive type " + primitive.name());
    }
  }

  /** Encodes an enum as its zero-based symbol index (§3.2). */
  private static void writeEnum(
      ByteArrayOutputStream out, EnumType enumType, Object value, String path) {
    String symbol = PayloadValues.requireText(value, path);
    int index = enumType.symbols().indexOf(symbol);
    if (index < 0) {
      throw new PayloadEncodingException(
          PayloadValues.at(path)
              + ": '"
              + symbol
              + "' is not a symbol of enum '"
              + enumType.name()
              + "'; declared symbols are "
              + enumType.symbols());
    }
    writeZigzagInt(out, index);
  }

  /** Encodes an array as a single block plus the zero terminator (§3.2). */
  private static void writeArray(
      ByteArrayOutputStream out, ArrayType arrayType, Object value, String path) {
    List<?> items = PayloadValues.requireList(value, path);
    if (!items.isEmpty()) {
      writeZigzagLong(out, items.size());
      for (int i = 0; i < items.size(); i++) {
        Object item = items.get(i);
        if (item == null) {
          throw new PayloadEncodingException(
              PayloadValues.at(path + "[" + i + "]") + ": value must not be null");
        }
        writeValue(out, arrayType.items(), item, path + "[" + i + "]");
      }
    }
    writeZigzagLong(out, 0L);
  }

  /**
   * Encodes a map as a single block plus the zero terminator, with keys in ascending UTF-8 byte
   * order (§3.2).
   *
   */
  private static void writeMap(
      ByteArrayOutputStream out, MapType mapType, Object value, String path) {
    Map<String, Object> entries = PayloadValues.requireMap(value, path);
    if (!entries.isEmpty()) {
      TreeMap<String, Object> sorted = new TreeMap<>(CanonicalJson.UTF8_ORDER);
      sorted.putAll(entries);
      writeZigzagLong(out, sorted.size());
      for (Map.Entry<String, Object> entry : sorted.entrySet()) {
        String key = payloadKey(entry.getKey(), path);
        if (entry.getValue() == null) {
          throw new PayloadEncodingException(
              PayloadValues.at(path + "['" + key + "']") + ": value must not be null");
        }
        writeLengthPrefixed(out, CanonicalJson.utf8Bytes(key));
        writeValue(out, mapType.values(), entry.getValue(), path + "['" + key + "']");
      }
    }
    writeZigzagLong(out, 0L);
  }

  /** Validates a map key. */
  private static String payloadKey(String key, String path) {
    if (!CanonicalJson.isWellFormed(key)) {
      throw new PayloadEncodingException(
          PayloadValues.at(path)
              + ": map key contains an unpaired surrogate and cannot be encoded as UTF-8");
    }
    return key;
  }

  /** Writes a zigzag LEB128 varint (§3.1). */
  private static void writeUnsignedVarint(ByteArrayOutputStream out, long value) {
    long remaining = value;
    while ((remaining & ~0x7fL) != 0) {
      out.write((int) ((remaining & 0x7f) | 0x80));
      remaining >>>= 7;
    }
    out.write((int) remaining);
  }

  /** Writes a zigzag 32-bit varint (§3.1: {@code zigzag(n) = (n &lt;&lt; 1) ^ (n &gt;&gt; 31)}). */
  private static void writeZigzagInt(ByteArrayOutputStream out, int value) {
    writeUnsignedVarint(out, ((long) value << 1) ^ (value >> 31));
  }

  /** Writes a zigzag 64-bit varint (§3.1). */
  private static void writeZigzagLong(ByteArrayOutputStream out, long value) {
    writeUnsignedVarint(out, (value << 1) ^ (value >> 63));
  }

  /** Writes a length-prefixed byte sequence (§3.1: {@code long} length, then the bytes). */
  private static void writeLengthPrefixed(ByteArrayOutputStream out, byte[] payload) {
    writeZigzagLong(out, payload.length);
    out.write(payload, 0, payload.length);
  }

  /** Writes a double as 8 little-endian IEEE 754 bytes (§3.1). */
  private static void writeDouble(ByteArrayOutputStream out, double value) {
    long bits = Double.doubleToRawLongBits(value);
    for (int i = 0; i < 8; i++) {
      out.write((int) (bits >>> (8 * i)) & 0xff);
    }
  }

  /** Converts an instant to epoch milliseconds (§3.1). */
  private static long epochMillis(Instant instant, String path) {
    try {
      long millis = instant.getNano() / 1_000_000;
      return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1000L), millis);
    } catch (ArithmeticException e) {
      throw new PayloadEncodingException(
          PayloadValues.at(path)
              + ": instant "
              + instant
              + " is outside the range of timestamp-millis",
          e);
    }
  }

  /** Decodes a record (§3.2). */
  private static Map<String, Object> readRecord(Reader reader, RecordType record, String path) {
    Map<String, Object> value = new LinkedHashMap<>();
    for (Field field : record.fields()) {
      String fieldPath = PayloadValues.childPath(path, field.name());
      value.put(field.name(), readValue(reader, field.type(), fieldPath));
    }
    return value;
  }

  /** Decodes a value of the given type. */
  private static Object readValue(Reader reader, TypeNode type, String path) {
    return switch (type) {
      case PrimitiveType primitive -> readPrimitive(reader, primitive, path);
      case EnumType enumType -> readEnum(reader, enumType, path);
      case ArrayType arrayType -> readArray(reader, arrayType, path);
      case MapType mapType -> readMap(reader, mapType, path);
      case RecordType recordType -> readRecord(reader, recordType, path);
      case EnvelopeType envelopeType -> readRecord(reader, envelopeType.record(), path);
    };
  }

  /** Decodes a primitive (§3.1). */
  private static Object readPrimitive(Reader reader, PrimitiveType primitive, String path) {
    return switch (primitive.name()) {
      case PrimitiveType.STRING -> reader.readString(PayloadValues.at(path));
      case PrimitiveType.BOOLEAN -> reader.readBoolean(PayloadValues.at(path));
      case PrimitiveType.INT -> reader.readZigzagInt(PayloadValues.at(path));
      case PrimitiveType.LONG -> reader.readZigzagLong(PayloadValues.at(path));
      case PrimitiveType.DOUBLE -> reader.readDouble(PayloadValues.at(path));
      case PrimitiveType.BYTES -> reader.readBytes(PayloadValues.at(path));
      case PrimitiveType.TIMESTAMP_MILLIS -> reader.readInstant(PayloadValues.at(path));
      default -> throw new IllegalStateException("unknown primitive type " + primitive.name());
    };
  }

  /** Decodes an enum index (§3.3: an index outside the symbols is rejected). */
  private static String readEnum(Reader reader, EnumType enumType, String path) {
    int index = reader.readZigzagInt(PayloadValues.at(path));
    if (index < 0 || index >= enumType.symbols().size()) {
      throw new PayloadDecodingException(
          PayloadValues.at(path)
              + ": enum index "
              + index
              + " is outside the "
              + enumType.symbols().size()
              + " symbol(s) of enum '"
              + enumType.name()
              + "' (SCHEMA.md §3.3)");
    }
    return enumType.symbols().get(index);
  }

  /** Decodes array blocks (§3.2: readers loop over blocks until the zero terminator). */
  private static List<Object> readArray(Reader reader, ArrayType arrayType, String path) {
    List<Object> items = new ArrayList<>();
    while (true) {
      int count = reader.readBlockCount(PayloadValues.at(path));
      if (count == 0) {
        return items;
      }
      for (int i = 0; i < count; i++) {
        items.add(readValue(reader, arrayType.items(), path + "[" + items.size() + "]"));
      }
    }
  }

  /** Decodes map blocks (§3.2). */
  private static Map<String, Object> readMap(Reader reader, MapType mapType, String path) {
    Map<String, Object> entries = new LinkedHashMap<>();
    while (true) {
      int count = reader.readBlockCount(PayloadValues.at(path));
      if (count == 0) {
        return entries;
      }
      for (int i = 0; i < count; i++) {
        String key = reader.readString("key of " + PayloadValues.at(path));
        if (entries.containsKey(key)) {
          throw new PayloadDecodingException(
              PayloadValues.at(path) + ": map key '" + key + "' appears more than once");
        }
        entries.put(key, readValue(reader, mapType.values(), path + "['" + key + "']"));
      }
    }
  }

  /** The byte-oriented input of §3 with the strictness rules of §3.3. */
  private static final class Reader {

    private final byte[] bytes;
    private int position;

    /**
     * Creates a reader.
     *
     * @param bytes the payload bytes
     */
    Reader(byte[] bytes) {
      this.bytes = bytes;
    }

    /**
     * The number of unread bytes.
     *
     * @return the byte count
     */
    int remaining() {
      return bytes.length - position;
    }

    /**
     * Reads one byte.
     *
     * @param what what is being read, for error messages
     * @return the unsigned byte value
     * @throws PayloadDecodingException if the input is exhausted
     */
    int readByte(String what) {
      if (position >= bytes.length) {
        throw new PayloadDecodingException(
            "truncated input: expected " + what + " at offset " + position + " (SCHEMA.md §3.3)");
      }
      return bytes[position++] & 0xff;
    }

    /**
     * Reads an unsigned LEB128 varint (§3.1).
     *
     * @param what what is being read, for error messages
     * @return the value
     * @throws PayloadDecodingException if the input is exhausted or the varint is not at most 10
     *     bytes encoding a 64-bit value
     */
    long readUnsignedVarint(String what) {
      long value = 0;
      for (int shift = 0; shift <= 63; shift += 7) {
        int group = readByte(what);
        if (shift == 63 && (group & 0x7f) > 1) {
          throw new PayloadDecodingException("varint overflow while reading " + what);
        }
        value |= ((long) (group & 0x7f)) << shift;
        if ((group & 0x80) == 0) {
          return value;
        }
      }
      throw new PayloadDecodingException("varint longer than 10 bytes while reading " + what);
    }

    /**
     * Reads a zigzag 64-bit varint.
     *
     * @param what what is being read, for error messages
     * @return the value
     */
    long readZigzagLong(String what) {
      long raw = readUnsignedVarint(what);
      return (raw >>> 1) ^ -(raw & 1);
    }

    /**
     * Reads a zigzag 32-bit varint.
     *
     * @param what what is being read, for error messages
     * @return the value
     * @throws PayloadDecodingException if the value does not fit in a signed 32-bit int
     */
    int readZigzagInt(String what) {
      long value = readZigzagLong(what);
      if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
        throw new PayloadDecodingException(
            "int value " + value + " out of range while reading " + what);
      }
      return (int) value;
    }

    /**
     * Reads a length-prefixed byte sequence.
     *
     * @param what what is being read, for error messages
     * @return the bytes
     * @throws PayloadDecodingException if the length is negative or the input is truncated
     */
    byte[] readBytes(String what) {
      int length = readLength(what);
      byte[] payload = new byte[length];
      System.arraycopy(bytes, position, payload, 0, length);
      position += length;
      return payload;
    }

    /**
     * Reads a length-prefixed UTF-8 string (§3.3: invalid UTF-8 is rejected).
     *
     * @param what what is being read, for error messages
     * @return the string
     * @throws PayloadDecodingException if the bytes are not valid UTF-8
     */
    String readString(String what) {
      byte[] payload = readBytes(what);
      try {
        return StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(payload))
            .toString();
      } catch (CharacterCodingException e) {
        throw new PayloadDecodingException(
            what + " is not valid UTF-8 (SCHEMA.md §3.3)", e);
      }
    }

    /**
     * Reads a boolean (§3.1: {@code 0x01} is true, {@code 0x00} is false).
     *
     * @param what what is being read, for error messages
     * @return the boolean
     * @throws PayloadDecodingException if the byte is neither {@code 0x00} nor {@code 0x01}
     */
    boolean readBoolean(String what) {
      int value = readByte(what);
      if (value == 1) {
        return true;
      }
      if (value == 0) {
        return false;
      }
      throw new PayloadDecodingException(
          "invalid boolean byte 0x"
              + Integer.toHexString(value)
              + " while reading "
              + what
              + "; expected 0x00 or 0x01 (SCHEMA.md §3.1)");
    }

    /**
     * Reads an 8-byte little-endian double (§3.1).
     *
     * @param what what is being read, for error messages
     * @return the double
     */
    double readDouble(String what) {
      long bits = 0;
      for (int i = 0; i < 8; i++) {
        bits |= ((long) readByte(what)) << (8 * i);
      }
      return Double.longBitsToDouble(bits);
    }

    /**
     * Reads a {@code timestamp-millis} value.
     *
     * @param what what is being read, for error messages
     * @return the instant
     */
    Instant readInstant(String what) {
      // every long is a representable epoch-millisecond value (Instant spans +/-3.15e16 seconds)
      return Instant.ofEpochMilli(readZigzagLong(what));
    }

    /**
     * Reads a block count (§3.2, §3.3).
     *
     * @param what what is being read, for error messages
     * @return the count, zero for the terminator
     * @throws PayloadDecodingException if the count is negative or exceeds the implementation limit
     */
    int readBlockCount(String what) {
      long count = readZigzagLong("block count of " + what);
      if (count < 0) {
        throw new PayloadDecodingException(
            "negative block count " + count + " while reading " + what + " (SCHEMA.md §3.3)");
      }
      if (count > MAX_BLOCK_ELEMENTS) {
        throw new PayloadDecodingException(
            "block count "
                + count
                + " while reading "
                + what
                + " exceeds the implementation limit of "
                + MAX_BLOCK_ELEMENTS);
      }
      return (int) count;
    }

  /** Reads a length prefix and validates it against the remaining input. */
    private int readLength(String what) {
      long length = readZigzagLong("length of " + what);
      if (length < 0) {
        throw new PayloadDecodingException(
            "negative length " + length + " while reading " + what);
      }
      if (length > remaining()) {
        throw new PayloadDecodingException(
            "truncated input: "
                + what
                + " needs "
                + length
                + " byte(s) but only "
                + remaining()
                + " remain (SCHEMA.md §3.3)");
      }
      return (int) length;
    }
  }
}
