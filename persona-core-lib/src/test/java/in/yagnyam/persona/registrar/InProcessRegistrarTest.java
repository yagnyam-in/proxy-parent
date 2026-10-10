package in.yagnyam.persona.registrar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaCertificate;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.TestCertificates;
import in.yagnyam.persona.TestSupport;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.CsrService;
import in.yagnyam.persona.crypto.Hash;
import in.yagnyam.persona.crypto.PemService;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for {@link InProcessRegistrar} (protocol §3.4, §7.1):
 * two-step registration, certificate chaining to the root, duplicate
 * requestId rejection, revocation with passphrase verification, and
 * certificate lookup semantics.
 */
class InProcessRegistrarTest {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private final InProcessRegistrar registrar = new InProcessRegistrar();

  private RegisteredPersona registerFreshPersona(InProcessRegistrar registrar, String passPhrase) {
    return PersonaRegistrationFlow.register(registrar, UUID.randomUUID().toString(), passPhrase);
  }

  @Nested
  class PersonaIdIssuance {

    @Test
    void issuesWellFormedUniqueIds() {
      String first = registrar.issuePersonaId("req-1");
      String second = registrar.issuePersonaId("req-2");
      assertThat(first).matches("p-[0-9a-f]{24}");
      assertThat(second).matches("p-[0-9a-f]{24}");
      assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsDuplicateRequestId() {
      registrar.issuePersonaId("req-dup");
      assertThatThrownBy(() -> registrar.issuePersonaId("req-dup"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Duplicate requestId");
    }

    @Test
    void rejectsNullRequestId() {
      assertThatThrownBy(() -> registrar.issuePersonaId(null))
          .isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class Registration {

    @Test
    void registersPersonaWithCertificateChainedToRoot() {
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");

      assertThat(registered.persona().isValid()).isTrue();
      assertThat(registered.certificate().owner())
          .isEqualTo(registered.persona().id().id());
      assertThat(registered.chain()).hasSize(2);
      assertThat(registered.chain().get(0)).isEqualTo(registered.certificate());
      assertThat(registered.chain().get(1)).isEqualTo(registrar.trustedRoots().get(0));

      PersonaCertificate root = registrar.trustedRoots().get(0);
      assertThat(root.owner()).isEqualTo("persona-registrar");
      assertThatCode(() -> registered.certificate().certificate().verify(root.certificate().getPublicKey()))
          .doesNotThrowAnyException();
      assertThat(registered.certificate().certificate().getIssuerX500Principal())
          .isEqualTo(root.certificate().getSubjectX500Principal());
      assertThatCode(() -> root.certificate().verify(root.certificate().getPublicKey()))
          .doesNotThrowAnyException();
    }

    @Test
    void issuedCertificateIsWithinValidityWindowNow() {
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");
      Instant now = Instant.now();
      assertThat(registered.certificate().validFrom()).isBeforeOrEqualTo(now);
      assertThat(registered.certificate().validTill()).isAfter(now);
    }

    @Test
    void secondPersonaRegistrationWorksIndependently() {
      RegisteredPersona first = registerFreshPersona(registrar, "pass-phrase-1");
      RegisteredPersona second = registerFreshPersona(registrar, "pass-phrase-2");
      assertThat(second.persona().id().id()).isNotEqualTo(first.persona().id().id());
      assertThat(second.certificate().serialNumber())
          .isNotEqualTo(first.certificate().serialNumber());
      assertThat(second.certificate().isValid()).isTrue();
    }

    @Test
    void rejectsRegistrationForUnknownRequestId() throws GeneralSecurityException {
      String personaId = registrar.issuePersonaId("req-known");
      KeyPair keyPair = TestSupport.rsaKeyPair();
      String csr = CsrService.instance().buildCsr(personaId, keyPair, "SHA256withRSA");
      Hash hash = CRYPTO.getHash("SHA-256", "pass".getBytes(StandardCharsets.UTF_8));
      assertThatThrownBy(() -> registrar.registerPersona("req-never-issued", personaId, hash, csr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown requestId");
    }

    @Test
    void rejectsDuplicateRequestIdForRegistration() throws GeneralSecurityException {
      String requestId = "req-duplicate-registration";
      String personaId = registrar.issuePersonaId(requestId);
      registrar.registerPersona(requestId, personaId, validHash("pass-phrase"), validCsrFor(personaId));
      // A second registration attempt with the same requestId is rejected,
      // even with a brand-new valid CSR and hash.
      assertThatThrownBy(() -> registrar.registerPersona(requestId, personaId,
          validHash("pass-phrase"), validCsrFor(personaId)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Duplicate requestId");
    }

    @Test
    void rejectsPersonaIdNotIssuedForRequest() {
      String personaId = registrar.issuePersonaId("req-mismatch");
      String csr = validCsrFor(personaId);
      assertThatThrownBy(() -> registrar.registerPersona("req-mismatch", "p-someone-else",
          validHash("pass-phrase"), csr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("was not issued for requestId");
    }

    @Test
    void rejectsCsrWithCommonNameNotMatchingPersonaId() {
      String personaId = registrar.issuePersonaId("req-cn-mismatch");
      String csrForOtherId = validCsrFor("p-other-id");
      assertThatThrownBy(() -> registrar.registerPersona("req-cn-mismatch", personaId,
          validHash("pass-phrase"), csrForOtherId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("does not match personaId");
    }

    @Test
    void rejectsCsrWithInvalidSignature() {
      String personaId = registrar.issuePersonaId("req-bad-signature");
      KeyPair signingKeyPair = TestSupport.rsaKeyPair();
      KeyPair otherKeyPair = TestSupport.rsaKeyPair();
      String tamperedCsr = PersonaRegistrationFlow.csrWithSwappedPublicKey(
          personaId, signingKeyPair, otherKeyPair.getPublic());
      assertThatThrownBy(() -> registrar.registerPersona("req-bad-signature", personaId,
          validHash("pass-phrase"), tamperedCsr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("CSR signature is invalid");
    }

    @Test
    void rejectsMalformedCsrPem() {
      String personaId = registrar.issuePersonaId("req-bad-pem");
      assertThatThrownBy(() -> registrar.registerPersona("req-bad-pem", personaId,
          validHash("pass-phrase"), "not a csr"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid CSR");
    }

    @Test
    void rejectsInvalidRevocationPassPhraseHash() {
      String personaId = registrar.issuePersonaId("req-bad-hash");
      String csr = validCsrFor(personaId);
      Hash badHash = Hash.of("SHA-256", "", "");
      assertThatThrownBy(() -> registrar.registerPersona("req-bad-hash", personaId, badHash, csr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid revocation passphrase hash");
    }

    private Hash validHash(String passPhrase) {
      try {
        return CRYPTO.getHash("SHA-256", passPhrase.getBytes(StandardCharsets.UTF_8));
      } catch (GeneralSecurityException e) {
        throw new IllegalStateException(e);
      }
    }

    private String validCsrFor(String personaId) {
      try {
        return CsrService.instance().buildCsr(personaId, TestSupport.rsaKeyPair(), "SHA256withRSA");
      } catch (GeneralSecurityException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Nested
  class Revocation {

    @Test
    void revokesWithCorrectPassPhrase() throws GeneralSecurityException {
      RegisteredPersona registered = registerFreshPersona(registrar, "correct-pass-phrase");
      String personaId = registered.persona().id().id();
      assertThat(registrar.isRevoked(personaId)).isFalse();
      assertThat(registrar.isRevoked(registered.certificate())).isFalse();

      registrar.revoke(personaId, "correct-pass-phrase");

      assertThat(registrar.isRevoked(personaId)).isTrue();
      assertThat(registrar.isRevoked(registered.certificate())).isTrue();
      assertThat(registrar.certificateOf(PersonaId.of(personaId))).isEmpty();
      assertThat(registrar.certificateOf(registered.persona().id())).isEmpty();
    }

    @Test
    void rejectsWrongPassPhraseWithoutRevoking() {
      RegisteredPersona registered = registerFreshPersona(registrar, "correct-pass-phrase");
      String personaId = registered.persona().id().id();
      assertThatThrownBy(() -> registrar.revoke(personaId, "wrong-pass-phrase"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("passphrase does not match");
      assertThat(registrar.isRevoked(personaId)).isFalse();
    }

    @Test
    void rejectsUnknownPersonaId() {
      assertThatThrownBy(() -> registrar.revoke("p-never-registered", "any"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown personaId");
    }

    @Test
    void rejectsNullArguments() {
      assertThatThrownBy(() -> registrar.revoke(null, "any"))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> registrar.revoke("p-x", null))
          .isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class CertificateLookup {

    @Test
    void resolvesPinnedExactThumbprint() {
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");
      PersonaCertificate certificate = registered.certificate();
      PersonaId pinned = PersonaId.of(certificate.owner(), certificate.sha256Thumbprint());
      Optional<PersonaCertificate> resolved = registrar.certificateOf(pinned);
      assertThat(resolved).contains(certificate);
    }

    @Test
    void returnsEmptyForPinnedMismatch() {
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");
      PersonaId wrongPin = PersonaId.of(registered.certificate().owner(), "ee".repeat(32));
      assertThat(registrar.certificateOf(wrongPin)).isEmpty();
    }

    @Test
    void resolvesUnpinnedToLatestCertificate() {
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");
      Optional<PersonaCertificate> resolved =
          registrar.certificateOf(PersonaId.of(registered.certificate().owner()));
      assertThat(resolved).contains(registered.certificate());
    }

    @Test
    void returnsEmptyForUnknownPersona() {
      assertThat(registrar.certificateOf(PersonaId.of("p-unknown"))).isEmpty();
    }

    @Test
    void hidesRevokedCertificatesEvenWhenPinned() throws GeneralSecurityException {
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");
      registrar.revoke(registered.persona().id().id(), "pass-phrase");
      assertThat(registrar.certificateOf(registered.persona().id())).isEmpty();
      assertThat(registrar.certificateOf(PersonaId.of(registered.persona().id().id()))).isEmpty();
    }
  }

  @Nested
  class DeterministicRoot {

    @Test
    void acceptsProvidedSelfSignedRoot() throws Exception {
      KeyPair rootKeyPair = TestSupport.rsaKeyPair();
      String rootKeyPem = PemService.instance().encodePrivateKey(rootKeyPair.getPrivate());
      String rootCertificatePem = PemService.instance().encodeCertificate(
          TestCertificates.selfSignedWithCommonName(rootKeyPair, "custom-root", Duration.ofDays(1)));
      InProcessRegistrar deterministic = new InProcessRegistrar(rootKeyPem, rootCertificatePem);

      assertThat(deterministic.trustedRoots()).hasSize(1);
      assertThat(deterministic.trustedRoots().get(0).owner()).isEqualTo("custom-root");
      RegisteredPersona registered = registerFreshPersona(deterministic, "pass-phrase");
      assertThatCode(() -> registered.certificate().certificate()
          .verify(deterministic.trustedRoots().get(0).certificate().getPublicKey()))
          .doesNotThrowAnyException();
    }

    @Test
    void rejectsRootKeyNotMatchingCertificate() throws Exception {
      KeyPair rootKeyPair = TestSupport.rsaKeyPair();
      String rootCertificatePem = PemService.instance().encodeCertificate(
          TestCertificates.selfSignedWithCommonName(rootKeyPair, "custom-root", Duration.ofDays(1)));
      String wrongKeyPem = PemService.instance().encodePrivateKey(TestSupport.rsaKeyPair().getPrivate());
      assertThatThrownBy(() -> new InProcessRegistrar(wrongKeyPem, rootCertificatePem))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("does not match");
    }

    @Test
    void rejectsRootCertificateThatIsNotSelfSigned() throws Exception {
      InProcessRegistrar other = new InProcessRegistrar();
      RegisteredPersona notARoot = registerFreshPersona(other, "pass-phrase");
      String keyPem = PemService.instance().encodePrivateKey(
          TestSupport.rsaKeyPair().getPrivate());
      String personaCertPem = notARoot.certificate().certificatePem();
      assertThatThrownBy(() -> new InProcessRegistrar(keyPem, personaCertPem))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("not self-signed");
    }
  }

  @Nested
  class ConfigurableValidity {

    @Test
    void issuesShortLivedCertificates() {
      InProcessRegistrar shortLived = new InProcessRegistrar(Duration.ofMillis(500));
      RegisteredPersona registered = registerFreshPersona(shortLived, "pass-phrase");
      // X.509 encodes notBefore/notAfter with second granularity (values are
      // rounded to the nearest second), so a 500 ms certificate's effective
      // window is at most ~1.5 seconds — decisively short of the 1-year
      // default. The two bounds can even round to the same second.
      assertThat(registered.certificate().validTill())
          .isBeforeOrEqualTo(registered.certificate().validFrom().plus(Duration.ofSeconds(2)));
    }

    @Test
    void rejectsNonPositiveValidity() {
      assertThatThrownBy(() -> new InProcessRegistrar(Duration.ZERO))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new InProcessRegistrar(Duration.ofSeconds(-1)))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
