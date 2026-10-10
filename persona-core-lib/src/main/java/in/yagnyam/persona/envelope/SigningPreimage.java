package in.yagnyam.persona.envelope;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The signing preimage of protocol §6.4 — the unambiguous byte sequence all
 * signature algorithms of an envelope's signature set are computed over.
 *
 * <p>Binding the payload bytes together with the envelope fields that
 * determine how the payload is interpreted is the protocol's type-confusion
 * defense: re-labeling {@code messageType}, {@code payloadFormat} or
 * {@code schemaId} changes the preimage, so a signature over one labeling
 * never verifies over another.
 *
 * <pre>
 * domain       = "persona.signed-envelope@v1"          // for SignedEnvelope
 *              | "persona.multi-signed-envelope@v1"   // for MultiSignedEnvelope
 *
 * preimage     = uvarint(len(domain))       || domain (UTF-8)
 *              || uvarint(len(messageType)) || messageType (UTF-8)
 *              || uvarint(len(payloadFormat)) || payloadFormat (UTF-8)
 *              || uvarint(len(schemaId))    || schemaId (UTF-8; empty if absent)
 *              || payload (raw bytes)
 * </pre>
 *
 * <p>{@code uvarint(n)} is the unsigned LEB128 encoding of the byte length
 * {@code n}: least-significant 7-bit group first, high bit set on every byte
 * but the last. Note that the payload is length-prefixed as well, so the
 * preimage is unambiguous even between payloads where one is a prefix of the
 * other.
 *
 * <p>The distinct {@code domain} constants are what prevent signature
 * transplant between single-signed and multi-signed envelopes carrying
 * identical payloads (protocol §6.4): the domain is the first field, so the
 * preimages differ from their first length prefix onward and a signature
 * over one never verifies over the other.
 *
 * <p>An absent {@code schemaId} is represented by the empty string
 * throughout (see {@link SignedEnvelope}), so it contributes
 * {@code uvarint(0)} — a single {@code 0x00} byte — exactly as §6.4
 * requires for the absent case.
 */
public final class SigningPreimage {

  /** §6.4 domain constant of {@link SignedEnvelope}. */
  public static final String SIGNED_ENVELOPE_DOMAIN = "persona.signed-envelope@v1";

  /** §6.4 domain constant of {@link MultiSignedEnvelope}. */
  public static final String MULTI_SIGNED_ENVELOPE_DOMAIN = "persona.multi-signed-envelope@v1";

  private SigningPreimage() {}

  /**
   * The §6.4 preimage for a {@link SignedEnvelope}.
   *
   * @param messageType   envelope message type, never {@code null}
   * @param payloadFormat envelope payload format, never {@code null}
   * @param schemaId      envelope schema id; the empty string when absent,
   *                      never {@code null}
   * @param payload       the exact payload bytes, never {@code null}
   * @return the preimage bytes
   */
  public static byte[] forSignedEnvelope(
      String messageType, String payloadFormat, String schemaId, byte[] payload) {
    return of(SIGNED_ENVELOPE_DOMAIN, messageType, payloadFormat, schemaId, payload);
  }

  /**
   * The §6.4 preimage for a {@link MultiSignedEnvelope}.
   *
   * @param messageType   envelope message type, never {@code null}
   * @param payloadFormat envelope payload format, never {@code null}
   * @param schemaId      envelope schema id; the empty string when absent,
   *                      never {@code null}
   * @param payload       the exact payload bytes, never {@code null}
   * @return the preimage bytes
   */
  public static byte[] forMultiSignedEnvelope(
      String messageType, String payloadFormat, String schemaId, byte[] payload) {
    return of(MULTI_SIGNED_ENVELOPE_DOMAIN, messageType, payloadFormat, schemaId, payload);
  }

  /**
   * The §6.4 preimage for an explicit {@code domain} (one of
   * {@link #SIGNED_ENVELOPE_DOMAIN} / {@link #MULTI_SIGNED_ENVELOPE_DOMAIN}).
   *
   * @param domain        domain constant, never {@code null}
   * @param messageType   envelope message type, never {@code null}
   * @param payloadFormat envelope payload format, never {@code null}
   * @param schemaId      envelope schema id; the empty string when absent,
   *                      never {@code null}
   * @param payload       the exact payload bytes, never {@code null}
   * @return the preimage bytes
   */
  public static byte[] of(
      String domain, String messageType, String payloadFormat, String schemaId, byte[] payload) {
    Objects.requireNonNull(domain, "domain");
    Objects.requireNonNull(messageType, "messageType");
    Objects.requireNonNull(payloadFormat, "payloadFormat");
    Objects.requireNonNull(schemaId, "schemaId");
    Objects.requireNonNull(payload, "payload");
    ByteArrayOutputStream out =
        new ByteArrayOutputStream(64 + domain.length() + messageType.length()
            + payloadFormat.length() + schemaId.length() + payload.length);
    writeLengthPrefixedUtf8(out, domain);
    writeLengthPrefixedUtf8(out, messageType);
    writeLengthPrefixedUtf8(out, payloadFormat);
    writeLengthPrefixedUtf8(out, schemaId);
    writeUvarint(out, payload.length);
    out.writeBytes(payload);
    return out.toByteArray();
  }

  /** Appends {@code uvarint(len(text)) || text} in UTF-8 (§6.4). */
  private static void writeLengthPrefixedUtf8(ByteArrayOutputStream out, String text) {
    byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
    writeUvarint(out, encoded.length);
    out.writeBytes(encoded);
  }

  /** Appends the unsigned LEB128 encoding of {@code length} (§6.4). */
  private static void writeUvarint(ByteArrayOutputStream out, long length) {
    if (length < 0) {
      throw new IllegalArgumentException("uvarint length must not be negative: " + length);
    }
    long remaining = length;
    do {
      int group = (int) (remaining & 0x7F);
      remaining >>>= 7;
      if (remaining != 0) {
        group |= 0x80;
      }
      out.write(group);
    } while (remaining != 0);
  }
}
