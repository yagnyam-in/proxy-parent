package in.yagnyam.persona.schema.codec;

import in.yagnyam.persona.schema.PersonaSchema;
import java.util.Map;

/**
 * A payload codec: a schema-driven bidirectional mapping between a payload value and its wire
 * bytes ({@code docs/SCHEMA.md} §3 and §4).
 *
 * <p>The value model is that of §4.2: a {@code Map<String,Object>} from field name to value, where
 * values are {@link String}, {@link Boolean}, {@link Integer}, {@link Long}, {@link Double},
 * {@code byte[]}, {@link java.time.Instant}, {@link java.util.List} or {@code Map<String,Object>}.
 * Enums are represented by their symbol string, and envelope fields by a map shaped like the
 * built-in envelope schema of §5.
 *
 * <p>Implementations MUST be stateless and therefore safe to share between threads.
 */
public interface PayloadCodec {

  /**
   * Encodes a payload value.
   *
   * @param schema the schema the value is interpreted against (the root record type)
   * @param value the payload value
   * @return the wire bytes
   * @throws PayloadEncodingException if a field is missing, unknown, of the wrong type, or cannot
   *     be represented in this codec
   */
  byte[] encode(PersonaSchema schema, Map<String, Object> value);

  /**
   * Decodes a payload.
   *
   * @param schema the schema the bytes are interpreted against (the root record type)
   * @param bytes the wire bytes
   * @return the decoded payload value
   * @throws PayloadDecodingException if the bytes are malformed for this codec and schema
   */
  Map<String, Object> decode(PersonaSchema schema, byte[] bytes);

  /**
   * The {@code payloadFormat} value that identifies this codec on an envelope
   * ({@code PERSONA-PROTOCOL.md} §6.1).
   *
   * @return the format identifier, e.g. {@code "persona-binary"}
   */
  String format();
}
