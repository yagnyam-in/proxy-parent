package in.yagnyam.proxy.server;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.server.model.PrivateKeyEntity;
import in.yagnyam.proxy.services.BcCryptographyService;
import in.yagnyam.proxy.services.CryptographyService;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Date;

public final class TestUtils {

  private static final CryptographyService cryptographyService = BcCryptographyService.builder()
      .build();

  public static Certificate sampleCertificate(String serialNumber) {
    return Certificate.builder()
        .serialNumber(serialNumber)
        .owner("OWNER")
        .sha256Thumbprint("SHA256")
        .validFrom(new Date())
        .validTill(new Date())
        .certificateEncoded("CERT")
        .subject("SUB")
        .build();
  }

  public static Certificate sampleCertificateForFingerPrint(String fingerPrint) {
    return Certificate.builder()
        .serialNumber("dummy")
        .sha256Thumbprint(fingerPrint)
        .owner("OWNER")
        .validFrom(new Date())
        .validTill(new Date())
        .certificateEncoded("CERT")
        .subject("SUB")
        .build();
  }

  public static PrivateKeyEntity samplePrivateKeyEntity(String id) {
    return PrivateKeyEntity.builder()
        .id(id)
        .email("email")
        .phoneNumber("phone")
        .build();
  }

  public static KeyPair generateKeyPair() throws GeneralSecurityException {

    KeyPairGenerator generator = KeyPairGenerator
        .getInstance(cryptographyService.getKeyGenerationAlgorithm());
    generator.initialize(1024, new SecureRandom());
    return generator.generateKeyPair();
  }

}
