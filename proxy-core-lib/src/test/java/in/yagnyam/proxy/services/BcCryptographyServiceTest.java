package in.yagnyam.proxy.services;

import in.yagnyam.proxy.CipherText;
import in.yagnyam.proxy.Hash;
import in.yagnyam.proxy.Hmac;
import in.yagnyam.proxy.TestUtils;
import in.yagnyam.proxy.config.ProxyVersion;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.Certificate;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BcCryptographyServiceTest {

    private final ProxyVersion proxyVersion = ProxyVersion.latestVersion();

    @Test
    public void testGenerateKeyPair() throws GeneralSecurityException {
        BcCryptographyService cryptographyService = BcCryptographyService.builder().build();
        assertNotNull(cryptographyService
                .generateKeyPair(proxyVersion.getKeyGenerationAlgorithm(), proxyVersion.getKeySize()));
    }

    @Test
    public void testGetSignatureAndVerifySignature()
            throws GeneralSecurityException, IOException, OperatorCreationException {
        BcCryptographyService cryptographyService = BcCryptographyService.builder().build();

        String signatureAlgorithm = ProxyVersion.latestVersion().getCertificateSignatureAlgorithm();
        KeyPair keyPair = cryptographyService
                .generateKeyPair(proxyVersion.getKeyGenerationAlgorithm(), proxyVersion.getKeySize());
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
    public void testAsymmetricEncryptAndDecrypt()
            throws GeneralSecurityException, IOException, OperatorCreationException {
        BcCryptographyService cryptographyService = BcCryptographyService.builder().build();

        KeyPair keyPair = cryptographyService
                .generateKeyPair(proxyVersion.getKeyGenerationAlgorithm(), proxyVersion.getKeySize());
        Certificate certificate = TestUtils.createCertificate(keyPair);
        String inputData = "Dummy data";
        String encryptionAlgorithm = ProxyVersion.latestVersion().getPreferredAsymmetricEncryptionAlgorithm();

        CipherText cipher = cryptographyService.encrypt(certificate, encryptionAlgorithm, inputData);
        System.out.println("Cipher: " + cipher);
        assertNotEquals(cipher, cryptographyService.encrypt(certificate, encryptionAlgorithm, inputData));
        assertEquals("Dummy data", cryptographyService.decrypt(keyPair.getPrivate(), cipher));
    }


    @Test
    public void testSymmetricEncryptAndDecrypt() throws GeneralSecurityException {
        BcCryptographyService cryptographyService = BcCryptographyService.builder().build();
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
        String inputData = "Dummy data";
        String encryptionAlgorithm = "AES/CTR/NoPadding";

        CipherText cipher = cryptographyService.encrypt(secretKey, encryptionAlgorithm, inputData);
        System.out.println("Cipher: " + cipher);
        assertNotEquals(cipher, cryptographyService.encrypt(secretKey, encryptionAlgorithm, inputData));
        assertEquals("Dummy data", cryptographyService.decrypt(secretKey, cipher));
    }


    @Test
    public void testHash()
            throws GeneralSecurityException {
        BcCryptographyService cryptographyService = BcCryptographyService.builder().build();
        String data = "Hello World!!";
        Hash hash = cryptographyService.getHash("SHA256", data);
        assertTrue(cryptographyService.verifyHash(data, hash));
        assertFalse(cryptographyService.verifyHash(data + ".", hash));
        System.out.println(hash);
    }

    @Test
    public void testGetHmac()
            throws GeneralSecurityException {
        BcCryptographyService cryptographyService = BcCryptographyService.builder().build();
        String key = "Secret Key";
        String data = "Hello World!!";
        Hmac hmac = cryptographyService.getHmac("HmacSHA256", key, data);
        assertTrue(cryptographyService.verifyHmac(key, data, hmac));
        assertFalse(cryptographyService.verifyHmac(key + ".", data, hmac));
        assertFalse(cryptographyService.verifyHmac(key, data + ".", hmac));
    }

}
