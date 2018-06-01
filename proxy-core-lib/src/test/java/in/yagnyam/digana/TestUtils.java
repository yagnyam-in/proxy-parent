package in.yagnyam.digana;

import in.yagnyam.digana.utils.ChequeDateUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.Assert.fail;

@Slf4j
public class TestUtils {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static final String KEY_GENERATION_ALGORITHM = "RSA";
  public static final String SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";
  public static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;
  public static final int KEY_SIZE = 2048;

  public static void expectException(Class<? extends Throwable> e, Runnable r) {
    boolean failure = false;
    try {
      r.run();
      failure = true;
    } catch (Throwable t) {
      if (e.isInstance(t)) {
        log.info("Caught exception", e);
      } else {
        fail("Expecting exception of type " + e + ". But found: " + t);
      }
    }
    if (failure) {
      fail("Expecting exception of type " + e);
    }
  }

  public static X509Certificate createCertificate(KeyPair keyPair)
      throws GeneralSecurityException, OperatorCreationException, IOException {
    Date startDate = ChequeDateUtils.today();
    Date expiryDate = ChequeDateUtils.daysAfterToday(10);
    BigInteger serialNumber = BigInteger.ONE;
    X500Name subjectName = new X500Name("CN=Dummy");
    X509v3CertificateBuilder certificateBuilder =
        new X509v3CertificateBuilder(
            subjectName,
            serialNumber,
            startDate,
            expiryDate,
            subjectName,
            createSubjectKeyIdentifier(keyPair.getPublic()));
    ContentSigner signer =
        new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .setProvider(PROVIDER_NAME)
            .build(keyPair.getPrivate());

    X509CertificateHolder certificateHolder = certificateBuilder.build(signer);
    return new JcaX509CertificateConverter()
        .setProvider(PROVIDER_NAME)
        .getCertificate(certificateHolder);
  }

  public static KeyPair generateKeyPair() throws GeneralSecurityException {
    KeyPairGenerator generator =
        KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM, PROVIDER_NAME);
    generator.initialize(KEY_SIZE, new SecureRandom());
    return generator.generateKeyPair();
  }

  private static SubjectPublicKeyInfo createSubjectKeyIdentifier(Key key) throws IOException {
    ASN1InputStream is = null;
    try {
      is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()));
      ASN1Sequence seq = (ASN1Sequence) is.readObject();
      return SubjectPublicKeyInfo.getInstance(seq);
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }
}