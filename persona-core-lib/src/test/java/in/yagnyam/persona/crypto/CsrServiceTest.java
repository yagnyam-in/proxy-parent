package in.yagnyam.persona.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.TestSupport;
import java.security.KeyPair;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CsrService} (protocol §7.1 step 2): CSR subject CN and
 * signature validity.
 */
class CsrServiceTest {

  private final CsrService csrService = CsrService.instance();

  @Test
  void buildsCsrWithSubjectCommonName() throws Exception {
    KeyPair keyPair = TestSupport.rsaKeyPair();
    String csrPem = csrService.buildCsr("p-csr-test", keyPair,
        PersonaVersionV0.instance().certificateSignatureAlgorithm());
    assertThat(csrPem).contains("BEGIN CERTIFICATE REQUEST");
    PKCS10CertificationRequest csr = PemService.instance().decodeCertificateRequest(csrPem);
    assertThat(csr.getSubject().getRDNs(BCStyle.CN)[0].getFirst().getValue().toString())
        .isEqualTo("p-csr-test");
  }

  @Test
  void buildsCsrWithVerifiableSignature() throws Exception {
    KeyPair keyPair = TestSupport.rsaKeyPair();
    String csrPem = csrService.buildCsr("p-csr-test", keyPair, "SHA256withRSA");
    PKCS10CertificationRequest csr = PemService.instance().decodeCertificateRequest(csrPem);
    assertThat(csr.isSignatureValid(new JcaContentVerifierProviderBuilder()
        .setProvider(BouncyCastle.provider())
        .build(csr.getSubjectPublicKeyInfo())))
        .isTrue();
  }

  @Test
  void csrsForSameSubjectDifferByRandomnessOfNothingButAreDeterministicPerKey() throws Exception {
    // A CSR is fully determined by subject + key pair: rebuilding is stable.
    KeyPair keyPair = TestSupport.rsaKeyPair();
    String first = csrService.buildCsr("p-csr-test", keyPair, "SHA256withRSA");
    String second = csrService.buildCsr("p-csr-test", keyPair, "SHA256withRSA");
    assertThat(first).isEqualTo(second);
  }

  @Test
  void rejectsPersonaIdThatCannotBeACommonName() {
    KeyPair keyPair = TestSupport.rsaKeyPair();
    // '#' is reserved by the persona-id grammar (protocol §3.2).
    assertThatThrownBy(() -> csrService.buildCsr("p-a#b", keyPair, "SHA256withRSA"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("#");
    assertThatThrownBy(() -> csrService.buildCsr("", keyPair, "SHA256withRSA"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullArguments() {
    KeyPair keyPair = TestSupport.rsaKeyPair();
    assertThatThrownBy(() -> csrService.buildCsr(null, keyPair, "SHA256withRSA"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> csrService.buildCsr("p-csr-test", null, "SHA256withRSA"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> csrService.buildCsr("p-csr-test", keyPair, null))
        .isInstanceOf(NullPointerException.class);
  }
}
