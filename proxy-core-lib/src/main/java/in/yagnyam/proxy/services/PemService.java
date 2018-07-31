package in.yagnyam.proxy.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import lombok.Builder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * PEM Services for encoding and decoding Cryptographic objects to/from String
 */
@Builder
public class PemService {

  private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();

  static {
    Security.addProvider(BC_PROVIDER);
  }

  private String asPemString(String name, byte[] content) throws IOException {
    PemObject pemObject = new PemObject(name, content);
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    JcaPEMWriter writer = new JcaPEMWriter(new PrintWriter(byteStream));
    writer.writeObject(pemObject);
    writer.close();
    return new String(byteStream.toByteArray(), "UTF-8");
  }

  private byte[] getPemContent(String encodedPemObject) throws IOException {
    try (PemReader pemReader = new PemReader(
        new InputStreamReader(new ByteArrayInputStream(encodedPemObject.getBytes("UTF-8"))))) {
      return pemReader.readPemObject().getContent();
    }
  }

  public PublicKey decodePublicKey(String algorithm, String publicKeyEncoded)
      throws GeneralSecurityException, IOException {
    KeyFactory factory = KeyFactory.getInstance(algorithm, BC_PROVIDER);
    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(getPemContent(publicKeyEncoded));
    return factory.generatePublic(publicKeySpec);
  }

  public String encodePublicKey(PublicKey publicKey) throws IOException {
    return encodePublicKey(publicKey, "RSA PUBLIC KEY");
  }

  public String encodePublicKey(PublicKey publicKey, String name) throws IOException {
    return asPemString(name, publicKey.getEncoded());
  }


  public PrivateKey decodePrivateKey(String algorithm, String privateKeyEncoded)
      throws GeneralSecurityException, IOException {
    KeyFactory factory = KeyFactory.getInstance(algorithm, BC_PROVIDER);
    PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(getPemContent(privateKeyEncoded));
    return factory.generatePrivate(privKeySpec);
  }

  public String encodePrivateKey(PrivateKey privateKey) throws IOException {
    return asPemString("RSA PRIVATE KEY", privateKey.getEncoded());
  }

  public X509Certificate decodeCertificate(String encodedCertificate)
      throws GeneralSecurityException, IOException {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(getPemContent(encodedCertificate));
    CertificateFactory factory = CertificateFactory.getInstance("X.509", BC_PROVIDER);
    return (X509Certificate) factory.generateCertificate(byteStream);
  }

  public String encodeCertificate(X509Certificate certificate)
      throws GeneralSecurityException, IOException {
    return asPemString("CERTIFICATE", certificate.getEncoded());
  }

  public PKCS10CertificationRequest decodeCertificateRequest(final String encodedCertificateRequest)
      throws IOException {
    return new PKCS10CertificationRequest(getPemContent(encodedCertificateRequest));
  }

  /**
   * Returns Certificate Request as PEM encoded String
   *
   * @param certificateRequest Certificate Request
   * @return PEM encoded string for Certificate Request
   * @throws IOException Any error while encoding
   */
  public String encodeCertificateRequest(final PKCS10CertificationRequest certificateRequest)
      throws IOException {
    return asPemString("CERTIFICATE REQUEST", certificateRequest.getEncoded());
  }

}
