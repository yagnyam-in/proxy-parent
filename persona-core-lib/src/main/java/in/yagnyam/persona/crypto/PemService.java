package in.yagnyam.persona.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * PEM encoding/decoding of cryptographic objects using BouncyCastle
 * (protocol §8; registration wire format §7.1).
 *
 * <p>Supported PEM objects:
 * <ul>
 *   <li>{@code PublicKey} — X.509 SubjectPublicKeyInfo ({@code PUBLIC KEY}),
 *       PKCS#1 ({@code RSA PUBLIC KEY}) accepted on decode</li>
 *   <li>{@code PrivateKey} — PKCS#8 ({@code PRIVATE KEY}), PKCS#1
 *       ({@code RSA PRIVATE KEY}) accepted on decode</li>
 *   <li>{@code X509Certificate} — {@code CERTIFICATE}</li>
 *   <li>{@code PKCS10CertificationRequest} — {@code CERTIFICATE REQUEST}
 *       (also {@code NEW CERTIFICATE REQUEST})</li>
 * </ul>
 *
 * <p>Decode methods parse only the FIRST PEM object in the input and ignore
 * any trailing content: a certificate chain file therefore yields the leaf
 * certificate only.
 *
 * <p>The service is stateless and thread-safe; use {@link #instance()}.
 */
public final class PemService {

  private static final PemService INSTANCE = new PemService();

  private final JcaPEMKeyConverter keyConverter =
      new JcaPEMKeyConverter().setProvider(BouncyCastle.provider());

  private PemService() {}

  /**
   * Returns the shared, thread-safe instance.
   *
   * @return singleton PEM service
   */
  public static PemService instance() {
    return INSTANCE;
  }

  /**
   * Encodes a public key as PEM (X.509 SubjectPublicKeyInfo).
   *
   * @param publicKey public key, never {@code null}
   * @return PEM string with a {@code PUBLIC KEY} header
   * @throws IOException if encoding fails
   */
  public String encodePublicKey(PublicKey publicKey) throws IOException {
    Objects.requireNonNull(publicKey, "publicKey");
    return asPem("PUBLIC KEY", publicKey.getEncoded());
  }

  /**
   * Decodes a public key from PEM (X.509 SubjectPublicKeyInfo, or PKCS#1 for
   * RSA).
   *
   * @param publicKeyPem PEM string
   * @return decoded public key
   * @throws GeneralSecurityException if the key cannot be decoded
   * @throws IOException if the input is not a valid PEM public key
   */
  public PublicKey decodePublicKey(String publicKeyPem) throws GeneralSecurityException, IOException {
    Objects.requireNonNull(publicKeyPem, "publicKeyPem");
    PemObject pemObject = readPem(publicKeyPem, "PUBLIC KEY", "RSA PUBLIC KEY");
    try {
      SubjectPublicKeyInfo publicKeyInfo;
      if ("PUBLIC KEY".equals(pemObject.getType())) {
        publicKeyInfo = SubjectPublicKeyInfo.getInstance(pemObject.getContent());
      } else {
        RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(pemObject.getContent());
        publicKeyInfo = new SubjectPublicKeyInfo(
            new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE), rsaPublicKey);
      }
      return keyConverter.getPublicKey(publicKeyInfo);
    } catch (IOException e) {
      throw new GeneralSecurityException("Unable to decode public key PEM", e);
    }
  }

  /**
   * Encodes a private key as PEM (PKCS#8).
   *
   * @param privateKey private key, never {@code null}
   * @return PEM string with a {@code PRIVATE KEY} header
   * @throws IOException if encoding fails
   */
  public String encodePrivateKey(PrivateKey privateKey) throws IOException {
    Objects.requireNonNull(privateKey, "privateKey");
    return asPem("PRIVATE KEY", privateKey.getEncoded());
  }

  /**
   * Decodes a private key from PEM (PKCS#8, or PKCS#1 for RSA).
   *
   * @param privateKeyPem PEM string
   * @return decoded private key
   * @throws GeneralSecurityException if the key cannot be decoded
   * @throws IOException if the input is not a valid PEM private key
   */
  public PrivateKey decodePrivateKey(String privateKeyPem) throws GeneralSecurityException, IOException {
    Objects.requireNonNull(privateKeyPem, "privatePem");
    PemObject pemObject = readPem(privateKeyPem, "PRIVATE KEY", "RSA PRIVATE KEY");
    try {
      if ("PRIVATE KEY".equals(pemObject.getType())) {
        return keyConverter.getPrivateKey(PrivateKeyInfo.getInstance(pemObject.getContent()));
      }
      RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(pemObject.getContent());
      PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(
          new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE), rsaPrivateKey);
      return keyConverter.getPrivateKey(privateKeyInfo);
    } catch (IOException e) {
      throw new GeneralSecurityException("Unable to decode private key PEM", e);
    }
  }

  /**
   * Encodes an X.509 certificate as PEM.
   *
   * @param certificate certificate, never {@code null}
   * @return PEM string with a {@code CERTIFICATE} header
   * @throws GeneralSecurityException if the certificate cannot be encoded
   * @throws IOException if encoding fails
   */
  public String encodeCertificate(X509Certificate certificate)
      throws GeneralSecurityException, IOException {
    Objects.requireNonNull(certificate, "certificate");
    return asPem("CERTIFICATE", certificate.getEncoded());
  }

  /**
   * Decodes an X.509 certificate from PEM.
   *
   * @param certificatePem PEM string
   * @return decoded certificate
   * @throws GeneralSecurityException if the X.509 certificate factory cannot
   *     be obtained
   * @throws IOException if the input is not a valid PEM certificate
   */
  public X509Certificate decodeCertificate(String certificatePem)
      throws GeneralSecurityException, IOException {
    Objects.requireNonNull(certificatePem, "certificatePem");
    PemObject pemObject = readPem(certificatePem, "CERTIFICATE");
    CertificateFactory factory = CertificateFactory.getInstance("X.509", BouncyCastle.provider());
    Certificate certificate;
    try {
      certificate = factory.generateCertificate(new ByteArrayInputStream(pemObject.getContent()));
    } catch (CertificateException e) {
      // BouncyCastle 1.86+ throws for a PEM body that holds no certificate
      // (e.g. an empty body); earlier releases returned null instead. Both
      // behaviors mean the same malformed input (protocol §10.1), so both are
      // normalized to the same IOException at the decode boundary — the
      // observable rejection channel must not depend on the provider release.
      throw new IOException("PEM CERTIFICATE body does not hold a certificate", e);
    }
    if (certificate == null) {
      // Providers that return null rather than throwing (BouncyCastle before
      // 1.86) when the body holds no certificate — e.g. an empty body. Reject
      // it at the decode boundary; a silent null would surface as an unrelated
      // failure far from the malformed input (protocol §9.1).
      throw new IOException("PEM CERTIFICATE body does not hold a certificate");
    }
    return (X509Certificate) certificate;
  }

  /**
   * Encodes a PKCS#10 certificate signing request as PEM.
   *
   * @param certificateRequest CSR, never {@code null}
   * @return PEM string with a {@code CERTIFICATE REQUEST} header
   * @throws IOException if encoding fails
   */
  public String encodeCertificateRequest(PKCS10CertificationRequest certificateRequest) throws IOException {
    Objects.requireNonNull(certificateRequest, "certificateRequest");
    return asPem("CERTIFICATE REQUEST", certificateRequest.getEncoded());
  }

  /**
   * Decodes a PKCS#10 certificate signing request from PEM.
   *
   * @param certificateRequestPem PEM string
   * @return decoded CSR
   * @throws IOException if the input is not a valid PEM certificate request
   */
  public PKCS10CertificationRequest decodeCertificateRequest(String certificateRequestPem) throws IOException {
    Objects.requireNonNull(certificateRequestPem, "certificateRequestPem");
    PemObject pemObject = readPem(certificateRequestPem, "CERTIFICATE REQUEST", "NEW CERTIFICATE REQUEST");
    return new PKCS10CertificationRequest(pemObject.getContent());
  }

  private static String asPem(String type, byte[] content) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      writer.writeObject(new PemObject(type, content));
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Reads the first PEM object from {@code pem}, requiring one of
   * {@code expectedTypes}.
   *
   * <p>Only the FIRST PEM object in the input is parsed; any trailing
   * content (further PEM objects or arbitrary text) is silently ignored —
   * a chain file yields the leaf certificate only.
   *
   * @param pem            PEM input
   * @param expectedTypes acceptable PEM type headers
   * @return the first PEM object
   * @throws IOException if the input holds no PEM object of an expected type
   */
  private static PemObject readPem(String pem, String... expectedTypes) throws IOException {
    try (PemReader reader = new PemReader(new InputStreamReader(
        new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
      PemObject pemObject;
      try {
        pemObject = reader.readPemObject();
      } catch (DecoderException e) {
        // Malformed base64 body: PemReader surfaces it as a runtime
        // exception; fold it into the declared "malformed PEM" contract.
        throw new IOException("Malformed PEM body: " + firstLine(pem), e);
      }
      if (pemObject == null) {
        throw new IOException("Not a valid PEM object: " + firstLine(pem));
      }
      if (Arrays.stream(expectedTypes).noneMatch(type -> type.equals(pemObject.getType()))) {
        throw new IOException("Unexpected PEM type " + pemObject.getType()
            + ", expected one of " + Arrays.toString(expectedTypes));
      }
      return pemObject;
    }
  }

  private static String firstLine(String pem) {
    return pem.lines().findFirst().map(line -> line.substring(0, Math.min(64, line.length()))).orElse("");
  }
}
