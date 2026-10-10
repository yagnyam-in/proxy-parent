package in.yagnyam.persona.envelope;

import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves the wire {@code payloadFormat} string of an envelope to the
 * codec that encodes/decodes its payload (protocol §6.1).
 *
 * <p>Known formats come from the codecs themselves
 * ({@link PayloadCodec#format()} — {@code json} and {@code persona-binary}),
 * so no format identifier is hard-coded here.
 *
 * <p><strong>Unknown formats are rejected, not ignored</strong> (fail
 * closed): a payload whose encoding is unknown cannot be decoded, and
 * protocol §7.3 step 3 makes an undecodable payload a hard rejection
 * (protocol §4: "There is no partial trust"). The rejection applies at
 * signing time as well, so a signer can never emit an envelope its peers
 * must silently drop. Never mix encodings within a single signed envelope:
 * {@code payloadFormat} defines exactly one encoding (protocol §10).
 */
public final class PayloadFormats {

  private static final JsonPayloadCodec JSON = new JsonPayloadCodec();
  private static final BinaryPayloadCodec BINARY = new BinaryPayloadCodec();

  private static final Map<String, PayloadCodec> CODECS =
      Map.of(JSON.format(), JSON, BINARY.format(), BINARY);

  private PayloadFormats() {}

  /**
   * Resolves the codec for a {@code payloadFormat} wire string.
   *
   * @param format payload format identifier, e.g. {@code json}
   * @return the codec for that format
   * @throws NullPointerException if {@code format} is {@code null}
   * @throws IllegalArgumentException if the format is unknown; the message
   *     lists the supported formats
   */
  public static PayloadCodec require(String format) {
    Objects.requireNonNull(format, "format");
    PayloadCodec codec = CODECS.get(format);
    if (codec == null) {
      throw new IllegalArgumentException(
          "unsupported payloadFormat '" + format + "'; supported formats are " + supportedFormats()
              + " (protocol §6.1: payloadFormat defines exactly one encoding of the payload;"
              + " unknown formats are rejected, not ignored)");
    }
    return codec;
  }

  /**
   * The supported payload format identifiers.
   *
   * @return the format identifiers of every known codec
   */
  public static Set<String> supportedFormats() {
    return CODECS.keySet();
  }
}
