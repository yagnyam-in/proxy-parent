package in.yagnyam.persona.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PayloadFormats} (protocol §6.1): known formats resolve
 * to their codec; unknown formats are rejected with the supported list
 * (fail closed, never ignored).
 */
class PayloadFormatsTest {

  @Test
  void resolvesTheJsonFormat() {
    assertThat(PayloadFormats.require("json")).isInstanceOf(JsonPayloadCodec.class);
    assertThat(PayloadFormats.require("json").format()).isEqualTo("json");
  }

  @Test
  void resolvesTheBinaryFormat() {
    assertThat(PayloadFormats.require("persona-binary")).isInstanceOf(BinaryPayloadCodec.class);
    assertThat(PayloadFormats.require("persona-binary").format()).isEqualTo("persona-binary");
  }

  @Test
  void supportedFormatsAreExactlyTheCodecFormats() {
    assertThat(PayloadFormats.supportedFormats())
        .containsExactlyInAnyOrder("json", "persona-binary");
  }

  @Test
  void rejectsUnknownFormats() {
    // protocol §10 names other formats ("avro", "protobuf"), but this
    // implementation ships no codec for them, so they are rejected — a
    // payload that cannot be decoded is a hard rejection (§4, §7.3 step 3)
    assertThatThrownBy(() -> PayloadFormats.require("avro"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsupported payloadFormat 'avro'")
        .hasMessageContaining("json")
        .hasMessageContaining("persona-binary");
    assertThatThrownBy(() -> PayloadFormats.require("protobuf"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNull() {
    assertThatThrownBy(() -> PayloadFormats.require(null))
        .isInstanceOf(NullPointerException.class);
  }
}
