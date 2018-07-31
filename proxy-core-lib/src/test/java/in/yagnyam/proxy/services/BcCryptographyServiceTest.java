package in.yagnyam.proxy.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import in.yagnyam.proxy.TestUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.Certificate;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BcCryptographyServiceTest {

  @Test
  public void testGenerateKeyPair() throws GeneralSecurityException {
    BcCryptographyService cryptographyService = BcCryptographyService.builder().build();
    assertNotNull(cryptographyService.generateKeyPair());
  }

  @Test
  public void testGetSignatureAndVerifySignature()
      throws GeneralSecurityException, IOException, OperatorCreationException {
    BcCryptographyService cryptographyService = BcCryptographyService.builder().build();

    String signatureAlgorithm = cryptographyService.getDefaultSignatureAlgorithm();
    KeyPair keyPair = cryptographyService.generateKeyPair();
    Certificate certificate = TestUtils.createCertificate(keyPair);
    String inputData = "Dummy data";

    String signature = cryptographyService
        .getSignature(signatureAlgorithm, keyPair.getPrivate(), inputData);
    System.out.println("Signature: " + signature);
    assertTrue(
        cryptographyService.verifySignature(signatureAlgorithm, certificate, inputData, signature));
    assertFalse(cryptographyService
        .verifySignature(signatureAlgorithm, certificate, inputData + ".", signature));
  }

  @Test
  public void testEncryptAndDecrypt()
      throws GeneralSecurityException, IOException, OperatorCreationException {
    BcCryptographyService cryptographyService = BcCryptographyService.builder().build();

    KeyPair keyPair = cryptographyService.generateKeyPair();
    Certificate certificate = TestUtils.createCertificate(keyPair);
    String inputData = "Dummy data";

    String cipher = cryptographyService
        .encrypt(cryptographyService.getDefaultEncryptionAlgorithm(), certificate, inputData);
    System.out.println("Cipher: " + cipher);
    assertNotEquals(cipher, cryptographyService
        .encrypt(cryptographyService.getDefaultEncryptionAlgorithm(), certificate, inputData));
    assertEquals("Dummy data", cryptographyService
        .decrypt(cryptographyService.getDefaultEncryptionAlgorithm(), keyPair.getPrivate(),
            cipher));
  }

}
