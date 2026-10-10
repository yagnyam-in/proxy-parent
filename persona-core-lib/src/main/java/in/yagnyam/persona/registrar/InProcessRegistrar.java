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
 *       requestId, validates the CSR and issues the certificate.</li>
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
    IssuedRequest request = new IssuedRequest(freshPersonaId());
    if (requestsByRequestId.putIfAbsent(requestId, request) != null) {
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
      String commonName = csrCommonName(csr);
      if (!commonName.equals(personaId)) {
        throw new IllegalArgumentException(
            "CSR subject CN " + commonName + " does not match personaId " + personaId);
      }
      PersonaCertificate certificate = issueCertificate(csr);
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
      if (!certificatesByPersonaId.containsKey(candidate)) {
        return candidate;
      }
    }
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

  private static String csrCommonName(PKCS10CertificationRequest csr) {
    RDN[] rdns = csr.getSubject().getRDNs(BCStyle.CN);
    if (rdns == null || rdns.length == 0) {
      throw new IllegalArgumentException("CSR subject has no CN");
    }
    AttributeTypeAndValue attributeTypeAndValue = rdns[0].getFirst();
    if (attributeTypeAndValue == null) {
      throw new IllegalArgumentException("CSR subject CN is empty");
    }
    return attributeTypeAndValue.getValue().toString();
  }

  private PersonaCertificate issueCertificate(PKCS10CertificationRequest csr)
      throws GeneralSecurityException {
    try {
      BigInteger serial = BigInteger.valueOf(serialCounter.incrementAndGet());
      Date notBefore = Date.from(Instant.now());
      Date notAfter = Date.from(notBefore.toInstant().plus(personaCertificateValidity));
      X509CertificateHolder holder = new X509v3CertificateBuilder(
              rootSubject, serial, notBefore, notAfter, csr.getSubject(), csr.getSubjectPublicKeyInfo())
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

  /** Signs a probe with the candidate key and verifies it against the root certificate. */
  private boolean matchesRootKey(X509Certificate root, PrivateKey key) {
    try {
      byte[] probe = new byte[32];
      secureRandom.nextBytes(probe);
      byte[] signature = cryptographyService.getSignature(version.certificateSignatureAlgorithm(), key, probe);
      return cryptographyService.verifySignature(version.certificateSignatureAlgorithm(), root, probe, signature);
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
