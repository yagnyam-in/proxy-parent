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
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
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

  private Hash validHash(String passPhrase) {
    try {
      return CRYPTO.getHash("SHA-256", passPhrase.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private Hash hashOf(String algorithm, String passPhrase) {
    try {
      return CRYPTO.getHash(algorithm, passPhrase.getBytes(StandardCharsets.UTF_8));
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

    /**
     * The requestId is checked before any id is minted, so repeated retries of
     * an already-issued request cannot grow the registrar's lifetime claimed-id
     * set (each rejected retry would otherwise reserve an unreachable id).
     */
    @Test
    void rejectedDuplicateRequestsDoNotClaimNewIds() {
      registrar.issuePersonaId("req-retry");
      int claimedAfterFirst = registrar.claimedPersonaIdCount();
      for (int i = 0; i < 25; i++) {
        assertThatThrownBy(() -> registrar.issuePersonaId("req-retry"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate requestId");
      }
      assertThat(registrar.claimedPersonaIdCount()).isEqualTo(claimedAfterFirst);
      // A new requestId still mints and claims a fresh id.
      assertThat(registrar.issuePersonaId("req-retry-next")).matches("p-[0-9a-f]{24}");
      assertThat(registrar.claimedPersonaIdCount()).isEqualTo(claimedAfterFirst + 1);
    }

    /**
     * Collision guard (protocol §7.1: ids are never reused): an id already
     * claimed — even by an issued-but-never-registered request — can never
     * be handed out again, so a collision can never overwrite another
     * request's revocation state.
     */
    @Test
    void neverIssuesAnAlreadyClaimedId() {
      assertThat(registrar.claimPersonaId("p-collision-guard")).isTrue();
      assertThat(registrar.claimPersonaId("p-collision-guard")).isFalse(); // issued, unregistered
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");
      assertThat(registrar.claimPersonaId(registered.persona().id().id())).isFalse(); // registered
      // Fresh ids keep flowing after collisions were rejected.
      assertThat(registrar.issuePersonaId("req-after-collision")).matches("p-[0-9a-f]{24}");
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

    /**
     * G3 (P1, certificate-subject smuggling): a subject whose single RDN is
     * multi-valued is rejected (protocol §7.1).
     *
     * <p>Both spellings are adversarial. {@code CN=<personaId>+E=attacker@…}
     * is the one that defeated the previous
     * {@code getRDNs(BCStyle.CN)[0].getFirst()} comparison: the extra
     * attribute sorts after CN inside the RDN encoding, so {@code getFirst()}
     * returned the <em>correct</em> CN while the certificate subject — copied
     * wholesale from the CSR — carried {@code E=attacker@…} as well.
     * {@code CN=<personaId>+OU=attacker} sorts CN last, so the old comparison
     * happened to fail there; it is kept as a second spelling of the same
     * shape.
     */
    @Test
    void rejectsCsrWithMultiValuedSubjectRdn() {
      String requestId = "req-multi-valued-rdn";
      String personaId = registrar.issuePersonaId(requestId);
      X500Name smuggledEmail = new X500Name(new RDN[] {
          new RDN(new AttributeTypeAndValue[] {
              new AttributeTypeAndValue(BCStyle.CN, new DERUTF8String(personaId)),
              new AttributeTypeAndValue(BCStyle.E,
                  new DERUTF8String("attacker@example.com"))})});
      X500Name smuggledOu = new X500Name(new RDN[] {
          new RDN(new AttributeTypeAndValue[] {
              new AttributeTypeAndValue(BCStyle.CN, new DERUTF8String(personaId)),
              new AttributeTypeAndValue(BCStyle.OU, new DERUTF8String("attacker"))})});

      for (X500Name adversarialSubject : List.of(smuggledEmail, smuggledOu)) {
        String csr = PersonaRegistrationFlow.csrWithSubject(adversarialSubject,
            TestSupport.rsaKeyPair());
        assertThatThrownBy(() -> registrar.registerPersona(requestId, personaId,
            validHash("pass-phrase"), csr))
            .as("multi-valued RDN %s must be rejected", adversarialSubject)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("multi-valued")
            .hasMessageContaining("§7.1");
      }
    }

    /** G3 (P1): a subject with extra RDNs beyond the persona id is rejected. */
    @Test
    void rejectsCsrWithExtraSubjectAttributes() {
      String requestId = "req-extra-subject-attributes";
      String personaId = registrar.issuePersonaId(requestId);
      String csr = PersonaRegistrationFlow.csrWithSubject(
          new X500Name("CN=" + personaId + ",OU=attacker"), TestSupport.rsaKeyPair());

      assertThatThrownBy(() -> registrar.registerPersona(requestId, personaId,
          validHash("pass-phrase"), csr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("2 RDNs")
          .hasMessageContaining("§7.1");
    }

    /** G3 (P1): a subject without a CN — or without any RDN at all — is rejected. */
    @Test
    void rejectsCsrWithoutCommonName() {
      String requestId = "req-no-cn";
      String personaId = registrar.issuePersonaId(requestId);
      List<X500Name> subjectsWithoutCn = List.of(
          new X500Name("OU=attacker"),
          new X500Name("O=attacker,C=DE"),
          new X500Name(new RDN[0]));

      for (X500Name subject : subjectsWithoutCn) {
        String csr = PersonaRegistrationFlow.csrWithSubject(subject, TestSupport.rsaKeyPair());
        assertThatThrownBy(() -> registrar.registerPersona(requestId, personaId,
            validHash("pass-phrase"), csr))
            .as("subject %s without CN must be rejected", subject)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CN")
            .hasMessageContaining("§7.1");
      }
    }

    /**
     * G3 (P1): the issued subject is built from the validated persona id, not
     * from the CSR subject — the certificate carries exactly
     * {@code CN=<personaId>} and no attacker-controlled DN material.
     */
    @Test
    void issuedCertificateSubjectIsExactlyTheValidatedPersonaId() throws Exception {
      RegisteredPersona registered = registerFreshPersona(registrar, "pass-phrase");
      X509Certificate issued = registered.certificate().certificate();
      String personaId = registered.certificate().owner();

      assertThat(issued.getSubjectX500Principal().getName()).isEqualTo("CN=" + personaId);
      X500Name subject = X500Name.getInstance(issued.getSubjectX500Principal().getEncoded());
      assertThat(subject.getRDNs()).hasSize(1);
      assertThat(subject.getRDNs()[0].size()).isEqualTo(1);
      assertThat(subject.getRDNs()[0].getFirst().getType()).isEqualTo(BCStyle.CN);
      assertThat(subject.getRDNs()[0].getFirst().getValue().toString()).isEqualTo(personaId);
      assertThat(registered.persona().id().id()).isEqualTo(personaId);
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

    /**
     * Protocol §7.1/§8: the revocation passphrase hash MUST use the active
     * persona version's preferred hash algorithm — an MD5 hash is rejected
     * even though it is structurally valid.
     */
    @Test
    void rejectsRevocationPassPhraseHashWithWrongAlgorithm() {
      String personaId = registrar.issuePersonaId("req-md5-hash");
      String csr = validCsrFor(personaId);
      Hash md5Hash = hashOf("MD5", "pass-phrase");
      assertThat(md5Hash.isValid()).isTrue();
      assertThatThrownBy(() -> registrar.registerPersona("req-md5-hash", personaId, md5Hash, csr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("MD5")
          .hasMessageContaining("SHA-256")
          .hasMessageContaining("§7.1");
    }

    /**
     * Protocol §7.1/§8: the CSR must be signed with an algorithm from the
     * active version's preferred signature algorithm set. SHA1withRSA
     * (OID 1.2.840.113549.1.1.5) and SHA512withRSA are both cryptographically
     * valid but outside v0's set.
     */
    @Test
    void rejectsCsrSignedWithAlgorithmOutsidePreferredSet() throws GeneralSecurityException {
      String personaId = registrar.issuePersonaId("req-weak-csr-signature");
      String sha1Csr = CsrService.instance().buildCsr(personaId, TestSupport.rsaKeyPair(),
          "SHA1withRSA");
      assertThatThrownBy(() -> registrar.registerPersona("req-weak-csr-signature", personaId,
          validHash("pass-phrase"), sha1Csr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("preferred signature algorithm set")
          .hasMessageContaining("§8")
          .hasMessageContaining("1.2.840.113549.1.1.5");

      // A rejected CSR does not consume the request: retrying with the
      // same requestId and a policy-compliant CSR succeeds.
      String sha256Csr = validCsrFor(personaId);
      assertThatCode(() -> registrar.registerPersona("req-weak-csr-signature", personaId,
          validHash("pass-phrase"), sha256Csr)).doesNotThrowAnyException();

      String personaId512 = registrar.issuePersonaId("req-sha512-csr-signature");
      String sha512Csr = CsrService.instance().buildCsr(personaId512, TestSupport.rsaKeyPair(),
          "SHA512withRSA");
      assertThatThrownBy(() -> registrar.registerPersona("req-sha512-csr-signature", personaId512,
          validHash("pass-phrase"), sha512Csr))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("preferred signature algorithm set");
    }
  }

  /**
   * Deterministic concurrency invariant (protocol §7.1: the Registrar MUST
   * reject a second request with the same requestId): when N threads race
   * {@code registerPersona} on the same requestId, exactly one registration
   * succeeds, every other thread gets the duplicate error, and the
   * registrar ends with exactly one certificate for the persona.
   */
  @Nested
  class ConcurrentRegistration {

    @Test
    void sameRequestIdConcurrentRegistrationsYieldExactlyOneCertificate() throws Exception {
      String requestId = "req-concurrent";
      String personaId = registrar.issuePersonaId(requestId);
      int threads = 8;
      ExecutorService executor = Executors.newFixedThreadPool(threads);
      try {
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<RegisteredPersona>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
          final String passPhrase = "pass-phrase-" + i;
          futures.add(executor.submit(() -> {
            startGate.await();
            return registrar.registerPersona(requestId, personaId,
                validHash(passPhrase), validCsrFor(personaId));
          }));
        }
        startGate.countDown();

        int successes = 0;
        int duplicates = 0;
        for (Future<RegisteredPersona> future : futures) {
          try {
            RegisteredPersona registered = future.get(60, TimeUnit.SECONDS);
            successes++;
            assertThat(registered.certificate().owner()).isEqualTo(personaId);
          } catch (ExecutionException e) {
            assertThat(e.getCause())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate requestId");
            duplicates++;
          }
        }
        assertThat(successes).as("exactly one registration succeeds").isEqualTo(1);
        assertThat(duplicates).as("all other threads see the duplicate error")
            .isEqualTo(threads - 1);
        assertThat(registrar.certificateCount(personaId))
            .as("registrar ends with one certificate")
            .isEqualTo(1);
        assertThat(registrar.certificateOf(PersonaId.of(personaId))).isPresent();
      } finally {
        executor.shutdownNow();
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

    /**
     * G4 (P1): a deterministic root whose critical {@code keyUsage} asserts
     * only {@code keyCertSign | cRLSign} — the ordinary shape of a CA root —
     * must be accepted. Verifying the key probe with
     * {@code Signature.initVerify(certificate)} throws
     * {@code InvalidKeyException} for such a certificate (the JDK refuses a
     * certificate that does not assert {@code digitalSignature}), so the
     * probe verifies with the root's public key instead.
     */
    @Test
    void acceptsProvidedSelfSignedRootWithCaOnlyKeyUsage() throws Exception {
      KeyPair rootKeyPair = TestSupport.rsaKeyPair();
      String rootKeyPem = PemService.instance().encodePrivateKey(rootKeyPair.getPrivate());
      X509Certificate rootCertificate = TestCertificates.selfSignedCaRoot(
          rootKeyPair, "custom-root", Duration.ofDays(1));
      String rootCertificatePem = PemService.instance().encodeCertificate(rootCertificate);
      // Precondition: the root is a real CA certificate and really excludes
      // digitalSignature (otherwise this test would prove nothing).
      assertThat(rootCertificate.getCriticalExtensionOIDs()).contains("2.5.29.15");
      assertThat(rootCertificate.getKeyUsage()).isNotNull();
      assertThat(rootCertificate.getKeyUsage()[0]).isFalse();

      InProcessRegistrar deterministic = new InProcessRegistrar(rootKeyPem, rootCertificatePem);

      assertThat(deterministic.trustedRoots().get(0).owner()).isEqualTo("custom-root");
      RegisteredPersona registered = registerFreshPersona(deterministic, "pass-phrase");
      assertThatCode(() -> registered.certificate().certificate()
          .verify(rootCertificate.getPublicKey())).doesNotThrowAnyException();
    }

    /** G4 (P1): the CA-only root still rejects a mismatched key. */
    @Test
    void rejectsMismatchedKeyForCaOnlyRoot() throws Exception {
      KeyPair rootKeyPair = TestSupport.rsaKeyPair();
      String rootCertificatePem = PemService.instance().encodeCertificate(
          TestCertificates.selfSignedCaRoot(rootKeyPair, "custom-root", Duration.ofDays(1)));
      String wrongKeyPem = PemService.instance().encodePrivateKey(
          TestSupport.rsaKeyPair().getPrivate());

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
