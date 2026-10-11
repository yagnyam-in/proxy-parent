package in.yagnyam.persona.registrar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.TestSupport;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.CsrService;
import in.yagnyam.persona.crypto.Hash;
import in.yagnyam.persona.crypto.PemService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.crypto.PersonaVersionV0;
import in.yagnyam.persona.crypto.PersonaVersionV1;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * {@link InProcessRegistrar} bound to Persona Version v1 (protocol §3.5, §7.1,
 * §8): Ed25519 root, Ed25519-signed certificates, and a CSR policy that follows
 * the bound version — including refusing the other version's key family.
 */
class InProcessRegistrarV1Test {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();
  private static final PersonaVersion V1 = PersonaVersionV1.instance();

  @Test
  void generatesAnEd25519Root() throws Exception {
    InProcessRegistrar registrar = new InProcessRegistrar(V1);
    assertThat(registrar.version()).isSameAs(V1);
    X509Certificate root = PemService.instance()
        .decodeCertificate(registrar.trustedRoots().get(0).certificatePem());
    assertThat(root.getPublicKey().getAlgorithm()).isIn("Ed25519", "EdDSA");
    assertThat(root.getSigAlgName()).isIn("Ed25519", "ED25519");
    root.verify(root.getPublicKey());
  }

  /** Mirror of {@link #generatesAnEd25519Root()}: a v0 registrar generates an RSA root. */
  @Test
  void generatesAnRsaRoot() throws Exception {
    InProcessRegistrar registrar = new InProcessRegistrar(PersonaVersionV0.instance());
    assertThat(registrar.version()).isSameAs(PersonaVersionV0.instance());
    X509Certificate root = PemService.instance()
        .decodeCertificate(registrar.trustedRoots().get(0).certificatePem());
    assertThat(root.getPublicKey().getAlgorithm()).isEqualTo("RSA");
    assertThat(root.getSigAlgName()).isIn("SHA256withRSA", "SHA256WITHRSA");
    root.verify(root.getPublicKey());
  }

  @Test
  void issuesEd25519CertificatesSignedByTheRoot() throws Exception {
    InProcessRegistrar registrar = new InProcessRegistrar(V1);
    KeyPair keyPair = TestSupport.signingKeyPair(V1);
    RegisteredPersona registered = register(registrar, V1, keyPair, "Ed25519");

    X509Certificate issued = PemService.instance()
        .decodeCertificate(registered.certificate().certificatePem());
    X509Certificate root = PemService.instance()
        .decodeCertificate(registrar.trustedRoots().get(0).certificatePem());
    issued.verify(root.getPublicKey());
    assertThat(issued.getPublicKey().getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    // digitalSignature only: RFC 8410 §5 forbids keyEncipherment for Ed25519.
    boolean[] keyUsage = issued.getKeyUsage();
    assertThat(keyUsage[0]).isTrue();
    assertThat(keyUsage[2]).isFalse();
  }

  @Test
  void v1RegistrarRejectsRsaCsr() {
    InProcessRegistrar registrar = new InProcessRegistrar(V1);
    assertThatThrownBy(() -> register(registrar, V1, TestSupport.rsaKeyPair(), "SHA256withRSA"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("preferred signature algorithm set");
  }

  @Test
  void v0RegistrarRejectsEd25519Csr() {
    InProcessRegistrar registrar = new InProcessRegistrar(PersonaVersionV0.instance());
    assertThatThrownBy(() -> register(
        registrar, PersonaVersionV0.instance(), TestSupport.signingKeyPair(V1), "Ed25519"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("preferred signature algorithm set");
  }

  @Test
  void v0RegistrarKeepsDualUseKeyUsageForRsa() throws Exception {
    InProcessRegistrar registrar = new InProcessRegistrar(PersonaVersionV0.instance());
    RegisteredPersona registered =
        register(registrar, PersonaVersionV0.instance(), TestSupport.rsaKeyPair(), "SHA256withRSA");
    boolean[] keyUsage = PemService.instance()
        .decodeCertificate(registered.certificate().certificatePem()).getKeyUsage();
    assertThat(keyUsage[0]).isTrue();
    assertThat(keyUsage[2]).isTrue();
  }

  /** A deterministic root must belong to the bound version's certificate signature family. */
  @Test
  void pemConstructorRejectsRootOfAnotherFamily() throws Exception {
    // A genuine v0-style RSA root: self-signed and key-matched, so only its
    // family keeps it out of a v1 registrar.
    KeyPair rootKeyPair = TestSupport.rsaKeyPair();
    String rootCertificatePem = PemService.instance().encodeCertificate(
        in.yagnyam.persona.TestCertificates.selfSignedWithCommonName(
            rootKeyPair, "persona-registrar", Duration.ofDays(30)));
    String rootKeyPem = PemService.instance().encodePrivateKey(rootKeyPair.getPrivate());
    assertThatThrownBy(() -> new InProcessRegistrar(V1, Duration.ofDays(1), rootKeyPem, rootCertificatePem))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("v1");
  }

  /** The mirror direction: a v0 registrar rejects a matching Ed25519 root on its family. */
  @Test
  void pemConstructorRejectsEd25519RootUnderV0() throws Exception {
    KeyPair rootKeyPair = TestSupport.signingKeyPair(V1);
    String rootCertificatePem = PemService.instance().encodeCertificate(
        in.yagnyam.persona.TestCertificates.selfSignedWithCommonName(
            rootKeyPair, "persona-registrar", Duration.ofDays(30)));
    String rootKeyPem = PemService.instance().encodePrivateKey(rootKeyPair.getPrivate());
    assertThatThrownBy(() -> new InProcessRegistrar(PersonaVersionV0.instance(), Duration.ofDays(1),
        rootKeyPem, rootCertificatePem))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("v0");
  }

  /** A v0 registrar accepts a deterministic root that matches its policy: RSA, key-matched. */
  @Test
  void pemConstructorAcceptsMatchingRsaRootUnderV0() throws Exception {
    KeyPair rootKeyPair = TestSupport.rsaKeyPair();
    String rootCertificatePem = PemService.instance().encodeCertificate(
        in.yagnyam.persona.TestCertificates.selfSignedWithCommonName(
            rootKeyPair, "persona-registrar", Duration.ofDays(30)));
    InProcessRegistrar registrar = new InProcessRegistrar(PersonaVersionV0.instance(),
        Duration.ofDays(1), PemService.instance().encodePrivateKey(rootKeyPair.getPrivate()),
        rootCertificatePem);
    RegisteredPersona registered = register(registrar, PersonaVersionV0.instance(),
        TestSupport.rsaKeyPair(), "SHA256withRSA");
    PemService.instance().decodeCertificate(registered.certificate().certificatePem())
        .verify(rootKeyPair.getPublic());
  }

  @Test
  void pemConstructorAcceptsMatchingEd25519Root() throws Exception {
    KeyPair rootKeyPair = TestSupport.signingKeyPair(V1);
    String rootCertificatePem = PemService.instance().encodeCertificate(
        in.yagnyam.persona.TestCertificates.selfSignedWithCommonName(
            rootKeyPair, "persona-registrar", Duration.ofDays(30)));
    InProcessRegistrar registrar = new InProcessRegistrar(V1, Duration.ofDays(1),
        PemService.instance().encodePrivateKey(rootKeyPair.getPrivate()), rootCertificatePem);
    RegisteredPersona registered = register(registrar, V1, TestSupport.signingKeyPair(V1), "Ed25519");
    PemService.instance().decodeCertificate(registered.certificate().certificatePem())
        .verify(rootKeyPair.getPublic());
  }

  private static RegisteredPersona register(InProcessRegistrar registrar, PersonaVersion version,
      KeyPair keyPair, String csrAlgorithm) throws Exception {
    String requestId = UUID.randomUUID().toString();
    String personaId = registrar.issuePersonaId(requestId);
    Hash passPhraseHash = CRYPTO.getHash(version.preferredHashAlgorithm(), "pass".getBytes(UTF_8));
    String csr = CsrService.instance().buildCsr(personaId, keyPair, csrAlgorithm);
    return registrar.registerPersona(requestId, personaId, passPhraseHash, csr);
  }
}
