package in.yagnyam.proxy.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import in.yagnyam.proxy.Hash;
import in.yagnyam.proxy.Hmac;
import in.yagnyam.proxy.TestUtils;
import in.yagnyam.proxy.config.ProxyVersion;

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
    public void testEncryptAndDecrypt()
            throws GeneralSecurityException, IOException, OperatorCreationException {
        BcCryptographyService cryptographyService = BcCryptographyService.builder().build();

        KeyPair keyPair = cryptographyService
                .generateKeyPair(proxyVersion.getKeyGenerationAlgorithm(), proxyVersion.getKeySize());
        Certificate certificate = TestUtils.createCertificate(keyPair);
        String inputData = "Dummy data";
        String encryptionAlgorithm = ProxyVersion.latestVersion().getPreferredEncryptionAlgorithm();

        String cipher = cryptographyService.encrypt(encryptionAlgorithm, certificate, inputData);
        System.out.println("Cipher: " + cipher);
        assertNotEquals(cipher,
                cryptographyService.encrypt(encryptionAlgorithm, certificate, inputData));
        assertEquals("Dummy data",
                cryptographyService.decrypt(encryptionAlgorithm, keyPair.getPrivate(), cipher));
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
