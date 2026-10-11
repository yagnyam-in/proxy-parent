package in.yagnyam.persona.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Tests for {@link Hkdf} against the RFC 5869 Appendix A SHA-256 test cases. */
class HkdfTest {

  private static final HexFormat HEX = HexFormat.of();

  @Test
  void rfc5869TestCase1() {
    byte[] okm = Hkdf.hkdfSha256(
        HEX.parseHex("0b".repeat(22)),
        HEX.parseHex("000102030405060708090a0b0c"),
        HEX.parseHex("f0f1f2f3f4f5f6f7f8f9"),
        42);
    assertThat(HEX.formatHex(okm)).isEqualTo(
        "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865");
  }

  @Test
  void rfc5869TestCase2LongInputs() {
    byte[] okm = Hkdf.hkdfSha256(range(0x00, 0x50), range(0x60, 0xb0), range(0xb0, 0x100), 82);
    assertThat(HEX.formatHex(okm)).isEqualTo(
        "b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c"
            + "59045a99cac7827271cb41c65e590e09da3275600c2f09b8367793a9aca3db71"
            + "cc30c58179ec3e87c14c01d5c1f3434f1d87");
  }

  /** Empty salt means a hash-length all-zero key (RFC 5869 §2.2). */
  @Test
  void rfc5869TestCase3EmptySaltAndInfo() {
    byte[] okm = Hkdf.hkdfSha256(HEX.parseHex("0b".repeat(22)), new byte[0], new byte[0], 42);
    assertThat(HEX.formatHex(okm)).isEqualTo(
        "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8");
  }

  @Test
  void rejectsOutOfRangeLengths() {
    assertThatThrownBy(() -> Hkdf.hkdfSha256(new byte[1], new byte[0], new byte[0], 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Hkdf.hkdfSha256(new byte[1], new byte[0], new byte[0], 255 * 32 + 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(Hkdf.hkdfSha256(new byte[1], new byte[0], new byte[0], 255 * 32)).hasSize(255 * 32);
  }

  @Test
  void rejectsNullInputs() {
    assertThatThrownBy(() -> Hkdf.hkdfSha256(null, new byte[0], new byte[0], 32))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Hkdf.hkdfSha256(new byte[0], null, new byte[0], 32))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Hkdf.hkdfSha256(new byte[0], new byte[0], null, 32))
        .isInstanceOf(NullPointerException.class);
  }

  private static byte[] range(int fromInclusive, int toExclusive) {
    byte[] bytes = new byte[toExclusive - fromInclusive];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (fromInclusive + i);
    }
    return bytes;
  }
}
