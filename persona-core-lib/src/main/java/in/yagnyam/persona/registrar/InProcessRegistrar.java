package in.yagnyam.persona.registrar;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaCertificate;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.crypto.BouncyCastle;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.Hash;
import in.yagnyam.persona.crypto.PemService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.crypto.PersonaVersionV0;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-process Registrar (protocol §3.4, §7.1): a real CA that signs real X.509
 * v3 certificates with its own root key, for tests, examples and
 * single-process deployments.
 *
 * <p>Two-step registration flow (protocol §7.1):
 * <ol>
 *   <li>{@link #issuePersonaId(String)} reserves a fresh id for a
 *       requestId,</li>
 *   <li>{@link #registerPersona(String, String, Hash, String)} consumes that
 *       requestId, validates the CSR (signature, signature-algorithm policy
 *       per protocol §8, subject: exactly one CN attribute holding the
 *       allocated persona id) and the revocation passphrase hash
 *       (algorithm per protocol §7.1/§8, hashed with a per-Persona salt),
 *       and issues the certificate, whose subject is built from the
 *       validated id alone.</li>
 * </ol>
 * The same {@code requestId} can never be reused for either step.
 *
 * <p>The default constructor generates a self-signed root (RSA 2048,
 * {@code CN=persona-registrar}, 10-year validity, signed with the active
 * Persona Version's certificate algorithm). The PEM constructors accept a
 * deterministic root for reproducible tests. Issued persona certificates are
 * valid 1 year by default, configurable for tests.
 *
 * <p>Thread safety: request bookkeeping is a {@link ConcurrentHashMap} with
 * atomic {@code putIfAbsent}; certificate issuance is serialized under an
 * internal lock; certificate lists are {@link CopyOnWriteArrayList} so
 * readers always see consistent snapshots in issuance order.
 */
public final class InProcessRegistrar implements PersonaRegistrar {

  private static final Logger logger = LoggerFactory.getLogger(InProcessRegistrar.class);

  /** Default validity of issued persona certificates (protocol §7.1). */
  private static final Duration DEFAULT_PERSONA_CERTIFICATE_VALIDITY = Duration.ofDays(365);

  /** Validity of the self-signed registrar root certificate. */
  private static final Duration ROOT_CERTIFICATE_VALIDITY = Duration.ofDays(3650);

  /** Subject CN of the registrar root (protocol §3.4). */
  static final String ROOT_COMMON_NAME = "persona-registrar";

  /** Persona ids: {@code p-} + 24 lowercase hex chars from SecureRandom. */
  private static final int PERSONA_ID_RANDOM_BYTES = 12;

  private final PersonaVersion version = PersonaVersionV0.instance();
  private final CryptographyService cryptographyService = BouncyCastleCryptographyService.instance();
  private final PemService pemService = PemService.instance();

  private final Duration personaCertificateValidity;
  private final PrivateKey rootPrivateKey;
  private final PersonaCertificate rootCertificate;
  private final X500Name rootSubject;
  private final List<PersonaCertificate> trustedRoots;

  private final ConcurrentHashMap<String, IssuedRequest> requestsByRequestId = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CopyOnWriteArrayList<PersonaCertificate>> certificatesByPersonaId =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, PersonaCertificate> certificatesBySerialNumber = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Hash> revocationPassPhraseHashes = new ConcurrentHashMap<>();
  private final Set<String> revokedPersonaIds = ConcurrentHashMap.newKeySet();

  /**
   * Every persona id ever issued by this registrar, registered or not — a
   * collision with an issued-but-unregistered id must be rejected just like
   * a collision with a registered one, so a later registration can never
   * overwrite an earlier request's revocation state (protocol §7.1: ids
   * are never reused).
   */
  private final Set<String> issuedPersonaIds = ConcurrentHashMap.newKeySet();

  /** Acceptable CSR signature algorithm OIDs, derived from the active
   *      version's preferred signature algorithm set (protocol §8); computed once. */
  private volatile Set<String> csrSignatureAlgorithmIds;

  private final AtomicLong serialCounter = new AtomicLong(1);
  private final Object issuanceLock = new Object();
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Creates a registrar with a freshly generated root CA and the default
   * (1 year) persona certificate validity.
   */
  public InProcessRegistrar() {
    this(DEFAULT_PERSONA_CERTIFICATE_VALIDITY);
  }

  /**
   * Creates a registrar with a freshly generated root CA and a configurable
   * persona certificate validity (for expiry tests).
   *
   * @param personaCertificateValidity validity window of issued certificates
   */
  public InProcessRegistrar(Duration personaCertificateValidity) {
    this.personaCertificateValidity = requireValidity(personaCertificateValidity);
    try {
      KeyPair rootKeyPair =
          cryptographyService.generateKeyPair(version.keyGenerationAlgorithm(), version.keySize());
      X509Certificate root = buildRootCertificate(rootKeyPair.getPublic(), rootKeyPair.getPrivate());
      this.rootPrivateKey = rootKeyPair.getPrivate();
      this.rootCertificate = PersonaCertificate.of(root);
      this.rootSubject = subjectNameOf(root);
      this.trustedRoots = List.of(this.rootCertificate);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Unable to create registrar root CA", e);
    }
  }

  /**
   * Creates a registrar around a deterministic root (for reproducible
   * tests), with the default persona certificate validity. The root must be
   * self-signed and the key must match the certificate.
   *
   * @param rootKeyPem         PEM encoded root private key
   * @param rootCertificatePem PEM encoded self-signed root certificate
   */
  public InProcessRegistrar(String rootKeyPem, String rootCertificatePem) {
    this(DEFAULT_PERSONA_CERTIFICATE_VALIDITY, rootKeyPem, rootCertificatePem);
  }

  /**
   * Creates a registrar around a deterministic root and a configurable
   * persona certificate validity.
   *
   * @param personaCertificateValidity validity window of issued certificates
   * @param rootKeyPem                 PEM encoded root private key
   * @param rootCertificatePem         PEM encoded self-signed root certificate
   */
  public InProcessRegistrar(Duration personaCertificateValidity, String rootKeyPem, String rootCertificatePem) {
    this.personaCertificateValidity = requireValidity(personaCertificateValidity);
    try {
      PrivateKey rootKey = pemService.decodePrivateKey(Objects.requireNonNull(rootKeyPem, "rootKeyPem"));
      X509Certificate root = pemService.decodeCertificate(Objects.requireNonNull(rootCertificatePem, "rootCertificatePem"));
      if (!isSelfSigned(root)) {
        throw new IllegalArgumentException("Root certificate is not self-signed");
      }
      if (!matchesRootKey(root, rootKey)) {
        throw new IllegalArgumentException("Root private key does not match the root certificate");
      }
      this.rootPrivateKey = rootKey;
      this.rootCertificate = PersonaCertificate.of(root);
      this.rootSubject = subjectNameOf(root);
      this.trustedRoots = List.of(this.rootCertificate);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalArgumentException("Invalid root key/certificate material", e);
    }
  }

  @Override
  public String issuePersonaId(String requestId) {
    Objects.requireNonNull(requestId, "requestId");
    // Check BEFORE minting so duplicate-requestId retries do not burn ids
    // into the lifetime issued-id set (memory would grow per rejected retry).
    IssuedRequest existing = requestsByRequestId.get(requestId);
    if (existing != null) {
      throw new IllegalArgumentException("Duplicate requestId: " + requestId);
    }
    IssuedRequest request = new IssuedRequest(freshPersonaId());
    IssuedRequest winner = requestsByRequestId.putIfAbsent(requestId, request);
    if (winner != null) {
      // Lost a genuine race with a concurrent first submission: free the claim.
      releasePersonaId(request.personaId());
      throw new IllegalArgumentException("Duplicate requestId: " + requestId);
    }
    return request.personaId();
  }

  @Override
  public RegisteredPersona registerPersona(String requestId, String personaId, Hash revocationPassPhraseHash,
      String csrPem) throws GeneralSecurityException {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(personaId, "personaId");
    Objects.requireNonNull(revocationPassPhraseHash, "revocationPassPhraseHash");
    Objects.requireNonNull(csrPem, "csrPem");
    if (!revocationPassPhraseHash.isValid()) {
      throw new IllegalArgumentException("Invalid revocation passphrase hash");
    }
    if (!version.preferredHashAlgorithm().equals(revocationPassPhraseHash.algorithm())) {
      throw new IllegalArgumentException("Revocation passphrase hash algorithm '"
          + revocationPassPhraseHash.algorithm() + "' is not the active persona version's"
          + " preferred hash algorithm '" + version.preferredHashAlgorithm()
          + "' (protocol §7.1, §8: the passphrase MUST be hashed with the"
          + " version's preferred hash algorithm)");
    }
    IssuedRequest request = requestsByRequestId.get(requestId);
    if (request == null) {
      throw new IllegalArgumentException("Unknown requestId: " + requestId);
    }
    if (!request.personaId().equals(personaId)) {
      throw new IllegalArgumentException(
          "personaId " + personaId + " was not issued for requestId " + requestId);
    }
    synchronized (issuanceLock) {
      if (request.registered()) {
        throw new IllegalArgumentException("Duplicate requestId: " + requestId);
      }
      PKCS10CertificationRequest csr = parseCsr(csrPem);
      if (!isCsrSignatureValid(csr)) {
        throw new IllegalArgumentException("CSR signature is invalid");
      }
      requireCsrSignatureAlgorithmInPolicy(csr);
      String commonName = requiredCsrCommonName(csr);
      if (!commonName.equals(personaId)) {
        throw new IllegalArgumentException(
            "CSR subject CN " + commonName + " does not match personaId " + personaId);
      }
      PersonaCertificate certificate = issueCertificate(csr, personaId);
      certificatesByPersonaId
          .computeIfAbsent(personaId, key -> new CopyOnWriteArrayList<>())
          .add(certificate);
      certificatesBySerialNumber.put(certificate.serialNumber(), certificate);
      revocationPassPhraseHashes.put(personaId, revocationPassPhraseHash);
      request.markRegistered();
      logger.info("Registered persona {} (serial {})", personaId, certificate.serialNumber());
      return new RegisteredPersona(Persona.of(certificate), certificate,
          List.of(certificate, rootCertificate));
    }
  }

  @Override
  public void revoke(String personaId, String revocationPassPhrase) throws GeneralSecurityException {
    Objects.requireNonNull(personaId, "personaId");
    Objects.requireNonNull(revocationPassPhrase, "revocationPassPhrase");
    Hash stored = revocationPassPhraseHashes.get(personaId);
    if (stored == null) {
      throw new IllegalArgumentException("Unknown personaId: " + personaId + " (nothing to revoke)");
    }
    if (!cryptographyService.verifyHash(revocationPassPhrase.getBytes(StandardCharsets.UTF_8), stored)) {
      throw new IllegalArgumentException("Revocation passphrase does not match for persona " + personaId);
    }
    revokedPersonaIds.add(personaId);
    logger.info("Revoked persona {}", personaId);
  }

  @Override
  public boolean isRevoked(String personaId) {
    return personaId != null && revokedPersonaIds.contains(personaId);
  }

  @Override
  public boolean isRevoked(PersonaCertificate certificate) {
    return certificate != null && isRevoked(certificate.owner());
  }

  @Override
  public List<PersonaCertificate> trustedRoots() {
    return trustedRoots;
  }

  @Override
  public Optional<PersonaCertificate> certificateOf(PersonaId personaId) {
    Objects.requireNonNull(personaId, "personaId");
    List<PersonaCertificate> certificates = certificatesByPersonaId.get(personaId.id());
    if (certificates == null || certificates.isEmpty()) {
      return Optional.empty();
    }
    if (isRevoked(personaId.id())) {
      return Optional.empty();
    }
    if (personaId.sha256Thumbprint().isPresent()) {
      String pinned = personaId.sha256Thumbprint().get();
      return certificates.stream()
          .filter(certificate -> certificate.sha256Thumbprint().equals(pinned))
          .findFirst();
    }
    // Unpinned: latest issued (issuance order).
    return Optional.of(certificates.get(certificates.size() - 1));
  }

  private String freshPersonaId() {
    while (true) {
      byte[] bytes = new byte[PERSONA_ID_RANDOM_BYTES];
      secureRandom.nextBytes(bytes);
      String candidate = "p-" + HexFormat.of().formatHex(bytes);
      if (claimPersonaId(candidate)) {
        return candidate;
      }
    }
  }

  /**
   * Atomically claims {@code personaId} as fresh: returns {@code true} only
   * if the id is neither already registered nor already issued to an
   * earlier (possibly still unregistered) request. Registering the claim in
   * the same atomic step guarantees a random-id collision can never hand
   * the same id (and therefore the same revocation state) to two requests
   * (protocol §7.1: ids are never reused).
   *
   * <p>Package-private so the collision guard can be tested deterministically
   * without predicting {@link SecureRandom} output.
   *
   * @param personaId candidate id
   * @return {@code true} iff the id was fresh and is now claimed
   */
  boolean claimPersonaId(String personaId) {
    Objects.requireNonNull(personaId, "personaId");
    return !certificatesByPersonaId.containsKey(personaId) && issuedPersonaIds.add(personaId);
  }

  /**
   * Releases a claim taken by {@link #claimPersonaId(String)} so an id minted
   * for a submission that lost the requestId race is not retained for the
   * registrar's lifetime. Only called before any certificate or request has
   * been recorded for the id.
   *
   * @param personaId id whose claim is released
   */
  void releasePersonaId(String personaId) {
    Objects.requireNonNull(personaId, "personaId");
    issuedPersonaIds.remove(personaId);
  }

  /**
   * Number of ids currently claimed by this registrar (registered or issued
   * but not yet registered). Package-private inspection hook for tests.
   *
   * @return count of claimed persona ids
   */
  int claimedPersonaIdCount() {
    return issuedPersonaIds.size();
  }

  /**
   * Number of certificates issued for {@code personaId} (0 when unknown).
   * Package-private inspection hook for tests.
   *
   * @param personaId persona id
   * @return count of issued certificates
   */
  int certificateCount(String personaId) {
    List<PersonaCertificate> certificates = certificatesByPersonaId.get(personaId);
    return certificates == null ? 0 : certificates.size();
  }

  private PKCS10CertificationRequest parseCsr(String csrPem) {
    try {
      return pemService.decodeCertificateRequest(csrPem);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid CSR", e);
    }
  }

  /** Verifies the CSR's self-declared signature with its own public key. */
  private boolean isCsrSignatureValid(PKCS10CertificationRequest csr) {
    try {
      ContentVerifierProvider verifierProvider = new JcaContentVerifierProviderBuilder()
          .setProvider(BouncyCastle.provider())
          .build(csr.getSubjectPublicKeyInfo());
      return csr.isSignatureValid(verifierProvider);
    } catch (OperatorCreationException | PKCSException e) {
      // Fail closed on any verification machinery failure.
      return false;
    }
  }

  /**
   * Rejects a CSR whose signature algorithm is not in the active Persona
   * Version's preferred signature algorithm set (protocol §7.1, §8): a
   * cryptographically valid CSR signed with an unsanctioned algorithm
   * (e.g. SHA-1) must not result in a certificate that peers might then
   * treat as sanctioned.
   *
   * <p>Mapping choice: the acceptable OIDs are derived from the policy's
   * JCA algorithm names by building a probe {@link ContentSigner} for each
   * name and reading its {@code AlgorithmIdentifier} OID — no OID↔JCA-name
   * string table is maintained, so the accepted set always follows
   * {@link PersonaVersion#preferredSignatureAlgorithmSet()} exactly.
   *
   * @param csr parsed CSR
   * @throws IllegalArgumentException when the CSR's signature algorithm is
   *     outside the active version's preferred set
   */
  private void requireCsrSignatureAlgorithmInPolicy(PKCS10CertificationRequest csr)
      throws GeneralSecurityException {
    String csrAlgorithmId = csr.getSignatureAlgorithm().getAlgorithm().getId();
    Set<String> allowedAlgorithmIds = allowedCsrSignatureAlgorithmIds();
    if (!allowedAlgorithmIds.contains(csrAlgorithmId)) {
      throw new IllegalArgumentException("CSR signature algorithm is not in the active"
          + " persona version's preferred signature algorithm set (protocol §7.1, §8):"
          + " CSR is signed with OID " + csrAlgorithmId + ", accepted algorithms "
          + version.preferredSignatureAlgorithmSet() + " (OIDs " + allowedAlgorithmIds + ")");
    }
  }

  /**
   * Acceptable CSR signature algorithm OIDs, derived once from the active
   * version's preferred signature algorithm set (see
   * {@link #requireCsrSignatureAlgorithmInPolicy}).
   */
  private Set<String> allowedCsrSignatureAlgorithmIds() throws GeneralSecurityException {
    Set<String> cached = csrSignatureAlgorithmIds;
    if (cached != null) {
      return cached;
    }
    Set<String> derived = new HashSet<>();
    try {
      for (String algorithm : version.preferredSignatureAlgorithmSet()) {
        ContentSigner probe = new JcaContentSignerBuilder(algorithm)
            .setProvider(BouncyCastle.provider())
            .build(rootPrivateKey);
        derived.add(probe.getAlgorithmIdentifier().getAlgorithm().getId());
      }
    } catch (OperatorCreationException e) {
      throw new GeneralSecurityException("Unable to derive CSR signature algorithm policy", e);
    }
    if (derived.isEmpty()) {
      throw new GeneralSecurityException(
          "Active persona version declares no preferred signature algorithms");
    }
    Set<String> immutable = Set.copyOf(derived);
    csrSignatureAlgorithmIds = immutable;
    return immutable;
  }

  /**
   * Extracts the subject CN of a CSR, requiring the most constrained subject
   * shape the registration protocol allows (protocol §7.1 step 2: the CSR
   * subject is exactly {@code CN=<personaId>}).
   *
   * <p>Exactly one RDN carrying exactly one attribute, of type CN, is
   * accepted. Anything richer — a multi-valued RDN such as
   * {@code CN=p-x+OU=attacker}, additional RDNs such as {@code CN=p-x,O=evil},
   * or a non-CN attribute — is rejected: the Registrar only ever copies the
   * allocated id into the issued subject, so extra subject material could
   * otherwise smuggle attacker-chosen DN attributes into a certificate that
   * peers treat as registrar-issued.
   *
   * @param csr parsed CSR
   * @return the CSR subject CN
   * @throws IllegalArgumentException when the CSR subject is not exactly one
   *     single-valued CN RDN
   */
  private static String requiredCsrCommonName(PKCS10CertificationRequest csr) {
    RDN[] rdns = csr.getSubject().getRDNs();
    if (rdns == null || rdns.length == 0) {
      throw new IllegalArgumentException(
          "CSR subject must be exactly one CN attribute holding the personaId (protocol §7.1),"
              + " but has no subject RDNs");
    }
    if (rdns.length != 1) {
      throw new IllegalArgumentException(
          "CSR subject must be exactly one CN attribute holding the personaId (protocol §7.1),"
              + " but has " + rdns.length + " RDNs: " + csr.getSubject());
    }
    RDN rdn = rdns[0];
    if (rdn.size() != 1) {
      throw new IllegalArgumentException(
          "CSR subject must be exactly one CN attribute holding the personaId (protocol §7.1),"
              + " but its RDN is multi-valued: " + csr.getSubject());
    }
    AttributeTypeAndValue attributeTypeAndValue = rdn.getFirst();
    if (attributeTypeAndValue == null || !BCStyle.CN.equals(attributeTypeAndValue.getType())) {
      throw new IllegalArgumentException(
          "CSR subject must be exactly one CN attribute holding the personaId (protocol §7.1),"
              + " but its only attribute is "
              + (attributeTypeAndValue == null ? "<empty>" : attributeTypeAndValue.getType())
              + ": " + csr.getSubject());
    }
    return attributeTypeAndValue.getValue().toString();
  }

  /**
   * Issues the persona certificate: subject {@code CN=personaId} built from
   * the <em>validated</em> id (never from the CSR subject, which is
   * caller-supplied DN material), public key and key-possession proof from
   * the CSR (protocol §7.1 step 3).
   *
   * @param csr       verified CSR supplying the certified public key
   * @param personaId id allocated for this request and confirmed as the CSR
   *                  subject CN
   * @return issued certificate
   * @throws GeneralSecurityException when the certificate cannot be built
   */
  private PersonaCertificate issueCertificate(PKCS10CertificationRequest csr, String personaId)
      throws GeneralSecurityException {
    try {
      BigInteger serial = BigInteger.valueOf(serialCounter.incrementAndGet());
      Date notBefore = Date.from(Instant.now());
      Date notAfter = Date.from(notBefore.toInstant().plus(personaCertificateValidity));
      X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
          .addRDN(BCStyle.CN, personaId)
          .build();
      X509CertificateHolder holder = new X509v3CertificateBuilder(
              rootSubject, serial, notBefore, notAfter, subject, csr.getSubjectPublicKeyInfo())
          .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
          .addExtension(Extension.keyUsage, true,
              new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))
          .build(rootSigner());
      X509Certificate certificate = new JcaX509CertificateConverter()
          .setProvider(BouncyCastle.provider())
          .getCertificate(holder);
      return PersonaCertificate.of(certificate);
    } catch (IOException | OperatorCreationException e) {
      throw new GeneralSecurityException("Unable to issue certificate", e);
    }
  }

  private ContentSigner rootSigner() throws OperatorCreationException {
    return new JcaContentSignerBuilder(version.certificateSignatureAlgorithm())
        .setProvider(BouncyCastle.provider())
        .build(rootPrivateKey);
  }

  private X509Certificate buildRootCertificate(PublicKey rootPublicKey, PrivateKey rootPrivateKey)
      throws GeneralSecurityException {
    try {
      X500Name subject = new X500Name("CN=" + ROOT_COMMON_NAME);
      Date notBefore = Date.from(Instant.now());
      Date notAfter = Date.from(notBefore.toInstant().plus(ROOT_CERTIFICATE_VALIDITY));
      ContentSigner signer = new JcaContentSignerBuilder(version.certificateSignatureAlgorithm())
          .setProvider(BouncyCastle.provider())
          .build(rootPrivateKey);
      X509CertificateHolder holder = new X509v3CertificateBuilder(
              subject, BigInteger.ONE, notBefore, notAfter, subject,
              SubjectPublicKeyInfo.getInstance(rootPublicKey.getEncoded()))
          .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
          .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))
          .build(signer);
      return new JcaX509CertificateConverter()
          .setProvider(BouncyCastle.provider())
          .getCertificate(holder);
    } catch (IOException | OperatorCreationException e) {
      throw new GeneralSecurityException("Unable to build root certificate", e);
    }
  }

  private static boolean isSelfSigned(X509Certificate root) {
    try {
      root.verify(root.getPublicKey());
      return true;
    } catch (GeneralSecurityException e) {
      return false;
    }
  }

  /**
   * Signs a probe with the candidate key and verifies it against the root
   * certificate's <em>public key</em>.
   *
   * <p>Verifying against the certificate object instead would apply X.509
   * {@code keyUsage} restrictions: {@code Signature.initVerify(Certificate)}
   * rejects a certificate whose critical {@code keyUsage} lacks
   * {@code digitalSignature} — which is exactly how an ordinary CA root is
   * built ({@code keyCertSign | cRLSign} only, protocol §3.4 rule 5) — so
   * probing with the bare public key is required for the key/certificate
   * match to be decided on cryptography alone, not on key-usage semantics.
   */
  private boolean matchesRootKey(X509Certificate root, PrivateKey key) {
    try {
      byte[] probe = new byte[32];
      secureRandom.nextBytes(probe);
      byte[] signature = cryptographyService.getSignature(version.certificateSignatureAlgorithm(), key, probe);
      return cryptographyService.verifySignature(version.certificateSignatureAlgorithm(),
          root.getPublicKey(), probe, signature);
    } catch (GeneralSecurityException e) {
      return false;
    }
  }

  private static X500Name subjectNameOf(X509Certificate certificate) {
    return X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
  }

  private static Duration requireValidity(Duration validity) {
    Objects.requireNonNull(validity, "personaCertificateValidity");
    if (validity.isZero() || validity.isNegative()) {
      throw new IllegalArgumentException("Certificate validity must be positive: " + validity);
    }
    return validity;
  }

  /** Bookkeeping for a two-step registration: requestId -> (personaId, registered?). */
  private static final class IssuedRequest {

    private final String personaId;
    private volatile boolean registered;

    private IssuedRequest(String personaId) {
      this.personaId = personaId;
    }

    private String personaId() {
      return personaId;
    }

    private boolean registered() {
      return registered;
    }

    private void markRegistered() {
      registered = true;
    }
  }
}
