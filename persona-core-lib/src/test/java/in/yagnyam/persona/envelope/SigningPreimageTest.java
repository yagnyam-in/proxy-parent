package in.yagnyam.persona.envelope;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * Golden-vector tests for the §6.4 signing preimage.
 *
 * <p>The expected bytes below were produced by an <em>independent</em>
 * implementation — {@code preimage_golden.py} at
 * {@code src/test/resources/in/yagnyam/persona/envelope/}, a small Python
 * script written from the normative text of §6.4 with its own unsigned
 * LEB128 encoder — and pasted here verbatim. They are not re-derived from
 * the Java code under test: these tests assert that {@link SigningPreimage}
 * reproduces exactly the bytes the Python encoder printed for the same
 * inputs. Re-run the script to re-generate the literals:
 *
 * <pre>python3 src/test/resources/in/yagnyam/persona/envelope/preimage_golden.py</pre>
 */
class SigningPreimageTest {

  private static final String MESSAGE_TYPE = "example.Greeting@v1";
  private static final String SCHEMA_ID = "0123456789012345678901234567890123456789";

  /**
   * Golden vector {@code single_small} (57 bytes): single-signed domain,
   * absent schemaId (empty string), 3-byte payload.
   */
  private static final String GOLDEN_SINGLE_SMALL =
      "1a706572736f6e612e7369676e65642d656e76656c6f7065407631"
          + "136578616d706c652e4772656574696e67407631046a736f6e0003010203";

  /**
   * Golden vector {@code multi_small} (63 bytes): identical fields under the
   * multi-signed domain — the only difference from {@code single_small}.
   */
  private static final String GOLDEN_MULTI_SMALL =
      "20706572736f6e612e6d756c74692d7369676e65642d656e76656c6f7065407631"
          + "136578616d706c652e4772656574696e67407631046a736f6e0003010203";

  /**
   * Golden vector {@code single_schema} (107 bytes): present schemaId (40
   * lowercase hex) and the {@code persona-binary} payload format.
   */
  private static final String GOLDEN_SINGLE_SCHEMA =
      "1a706572736f6e612e7369676e65642d656e76656c6f7065407631"
          + "136578616d706c652e4772656574696e67407631"
          + "0e706572736f6e612d62696e617279"
          + "2830313233343536373839303132333435363738393031323334353637383930313233343536373839"
          + "03010203";

  /**
   * Golden vector {@code single_200} (255 bytes): a 200-byte payload, whose
   * length crosses the one-byte uvarint boundary and is encoded as the
   * two-byte sequence {@code c8 01}.
   */
  private static final String GOLDEN_SINGLE_200 =
      "1a706572736f6e612e7369676e65642d656e76656c6f7065407631"
          + "136578616d706c652e4772656574696e67407631046a736f6e00c801"
          + "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
          + "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f"
          + "404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f"
          + "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f"
          + "808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f"
          + "a0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebf"
          + "c0c1c2c3c4c5c6c7";

  /**
   * Golden vector {@code multi_empty} (110 bytes): empty payload under the
   * multi-signed domain — {@code uvarint(0)} is the single byte {@code 00}.
   */
  private static final String GOLDEN_MULTI_EMPTY =
      "20706572736f6e612e6d756c74692d7369676e65642d656e76656c6f7065407631"
          + "136578616d706c652e4772656574696e67407631"
          + "0e706572736f6e612d62696e617279"
          + "2830313233343536373839303132333435363738393031323334353637383930313233343536373839"
          + "00";

  private static byte[] payloadOfLength(int length) {
    byte[] payload = new byte[length];
    for (int i = 0; i < length; i++) {
      payload[i] = (byte) (i % 256);
    }
    return payload;
  }

  @Test
  void goldenVectorSingleSignedEnvelopeWithAbsentSchemaId() {
    byte[] preimage =
        SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1, 2, 3});

    assertThat(bytesToHex(preimage)).isEqualTo(GOLDEN_SINGLE_SMALL);
  }

  @Test
  void goldenVectorMultiSignedEnvelopeCarriesTheDistinctDomain() {
    byte[] preimage =
        SigningPreimage.forMultiSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1, 2, 3});

    assertThat(bytesToHex(preimage)).isEqualTo(GOLDEN_MULTI_SMALL);
  }

  @Test
  void goldenVectorWithPresentSchemaIdAndBinaryPayloadFormat() {
    byte[] preimage = SigningPreimage.forSignedEnvelope(
        MESSAGE_TYPE, "persona-binary", SCHEMA_ID, new byte[] {1, 2, 3});

    assertThat(bytesToHex(preimage)).isEqualTo(GOLDEN_SINGLE_SCHEMA);
  }

  @Test
  void goldenVector200BytePayloadCrossesTheOneByteVarintBoundary() {
    byte[] preimage = SigningPreimage.forSignedEnvelope(
        MESSAGE_TYPE, "json", "", payloadOfLength(200));

    assertThat(bytesToHex(preimage)).isEqualTo(GOLDEN_SINGLE_200);
    assertThat(preimage).hasSize(255);
  }

  @Test
  void goldenVectorEmptyPayloadUnderTheMultiSignedDomain() {
    byte[] preimage = SigningPreimage.forMultiSignedEnvelope(
        MESSAGE_TYPE, "persona-binary", SCHEMA_ID, new byte[0]);

    assertThat(bytesToHex(preimage)).isEqualTo(GOLDEN_MULTI_EMPTY);
  }

  @Test
  void theLengthPrefixesAreHandCheckable() {
    // domain "persona.signed-envelope@v1" is 26 bytes -> 0x1a;
    // messageType "example.Greeting@v1" is 19 bytes -> 0x13;
    // payloadFormat "json" is 4 bytes -> 0x04;
    // schemaId "" is 0 bytes -> 0x00.
    byte[] preimage =
        SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1, 2, 3});

    assertThat(preimage[0]).isEqualTo((byte) 0x1a);
    assertThat(new String(preimage, 1, 26, UTF_8))
        .isEqualTo(SigningPreimage.SIGNED_ENVELOPE_DOMAIN);
    assertThat(preimage[27]).isEqualTo((byte) 0x13);
    assertThat(new String(preimage, 28, 19, UTF_8)).isEqualTo(MESSAGE_TYPE);
    assertThat(preimage[47]).isEqualTo((byte) 0x04);
    assertThat(new String(preimage, 48, 4, UTF_8)).isEqualTo("json");
    assertThat(preimage[52]).isEqualTo((byte) 0x00);
    assertThat(preimage[53]).isEqualTo((byte) 0x03);
    // 1 + 26 + 1 + 19 + 1 + 4 + 1 + 0 + 1 + 3 = 57
    assertThat(preimage).hasSize(57);
  }

  @Test
  void theTwoDomainsAreDistinctConstants() {
    assertThat(SigningPreimage.SIGNED_ENVELOPE_DOMAIN).isEqualTo("persona.signed-envelope@v1");
    assertThat(SigningPreimage.MULTI_SIGNED_ENVELOPE_DOMAIN)
        .isEqualTo("persona.multi-signed-envelope@v1");
  }

  @Test
  void domainSeparationChangesThePreimageOfIdenticalEnvelopes() {
    byte[] single =
        SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1});
    byte[] multi =
        SigningPreimage.forMultiSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1});

    assertThat(single).isNotEqualTo(multi);
    // The divergence starts at the very first length prefix: 26 vs 32.
    assertThat(single[0]).isEqualTo((byte) 26);
    assertThat(multi[0]).isEqualTo((byte) 32);
  }

  @Test
  void everyTypeBindingFieldChangesThePreimage() {
    byte[] original = SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1});

    assertThat(SigningPreimage.forSignedEnvelope("example.Other@v1", "json", "", new byte[] {1}))
        .isNotEqualTo(original);
    assertThat(
            SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "persona-binary", "", new byte[] {1}))
        .isNotEqualTo(original);
    assertThat(SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", SCHEMA_ID, new byte[] {1}))
        .isNotEqualTo(original);
    assertThat(SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {2}))
        .isNotEqualTo(original);
  }

  @Test
  void thePayloadIsLengthPrefixedSoPrefixPayloadsCannotBeConfused() {
    byte[] one =
        SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1, 2});
    byte[] longer =
        SigningPreimage.forSignedEnvelope(MESSAGE_TYPE, "json", "", new byte[] {1, 2, 3});

    assertThat(one).isNotEqualTo(longer);
    assertThat(longer).hasSize(one.length + 1);
  }

  @Test
  void rejectsNulls() {
    byte[] payload = new byte[0];
    assertThatThrownBy(() -> SigningPreimage.of(null, MESSAGE_TYPE, "json", "", payload))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
        () -> SigningPreimage.of(SigningPreimage.SIGNED_ENVELOPE_DOMAIN, null, "json", "", payload))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
        () -> SigningPreimage.of(
            SigningPreimage.SIGNED_ENVELOPE_DOMAIN, MESSAGE_TYPE, null, "", payload))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
        () -> SigningPreimage.of(
            SigningPreimage.SIGNED_ENVELOPE_DOMAIN, MESSAGE_TYPE, "json", null, payload))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
        () -> SigningPreimage.of(
            SigningPreimage.SIGNED_ENVELOPE_DOMAIN, MESSAGE_TYPE, "json", "", (byte[]) null))
        .isInstanceOf(NullPointerException.class);
  }

  private static String bytesToHex(byte[] bytes) {
    return HexFormat.of().formatHex(bytes);
  }
}
