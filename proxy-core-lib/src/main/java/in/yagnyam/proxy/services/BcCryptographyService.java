package in.yagnyam.proxy.services;

import in.yagnyam.proxy.CipherText;
import in.yagnyam.proxy.Hash;
import in.yagnyam.proxy.Hmac;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Cryptography Service using Bouncy Castle
 */
@Slf4j
@Builder
public class BcCryptographyService implements CryptographyService {

    private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();
    private static final Map<String, String> encryptionAlgorithmMapping = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;

        {
            put("RSA/NONE/OAEPwithSHA-256andMGF1Padding", "RSA/NONE/OAEPwithSHA-256andMGF1Padding");
            put("AES/CTR/NoPadding", "AES/CTR/NoPadding");
        }
    };

    static {
        Security.addProvider(BC_PROVIDER);
    }

    @Override
    public KeyPair generateKeyPair(String keyGenerationAlgorithm, int keySize)
            throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(keyGenerationAlgorithm, BC_PROVIDER);
        generator.initialize(keySize, new SecureRandom());
        return generator.generateKeyPair();
    }

    @Override
    public Hash getHash(String hashAlgorithm, String message) throws GeneralSecurityException {
        byte[] iv = CryptographyService.randomIv(32);
        MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
        digest.update(iv);
        digest.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] hash = digest.digest();

        return Hash.builder()
                .algorithm(hashAlgorithm)
                .iv(Base64.toBase64String(iv))
                .hash(Base64.toBase64String(hash))
                .build();
    }

    @Override
    public boolean verifyHash(String message, Hash hash) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance(hash.getAlgorithm());
        digest.update(Base64.decode(hash.getIv()));
        digest.update(message.getBytes(StandardCharsets.UTF_8));
        return Arrays.areEqual(Base64.decode(hash.getHash()), digest.digest());
    }


    @Override
    public Hmac getHmac(String hmacAlgorithm, String key, String message) throws GeneralSecurityException {
        byte[] iv = new byte[32];
        new SecureRandom().nextBytes(iv);
        Mac mac = Mac.getInstance(hmacAlgorithm);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), hmacAlgorithm);
        mac.init(secretKey);
        mac.update(iv);
        mac.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] hmac = mac.doFinal();

        return Hmac.builder()
                .algorithm(hmacAlgorithm)
                .iv(Base64.toBase64String(iv))
                .hmac(Base64.toBase64String(hmac))
                .build();
    }

    @Override
    public boolean verifyHmac(String key, String message, Hmac hmac) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(hmac.getAlgorithm());
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), hmac.getAlgorithm());
        mac.init(secretKey);
        mac.update(Base64.decode(hmac.getIv()));
        mac.update(message.getBytes(StandardCharsets.UTF_8));
        return Arrays.areEqual(Base64.decode(hmac.getHmac()), mac.doFinal());
    }


    @Override
    public String getSignature(String algorithm, PrivateKey privateKey, String input)
            throws GeneralSecurityException {
        try {
            Signature signature = getSignatureInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(input.getBytes(StandardCharsets.UTF_8));
            return Base64.toBase64String(signature.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Error signing for algorithm " + algorithm, e);
            throw new GeneralSecurityException(e);
        }
    }

    @Override
    public boolean verifySignature(String algorithm, Certificate certificate, String input,
                                   String signature)
            throws GeneralSecurityException {
        try {
            Signature signatureInstance = getSignatureInstance(algorithm);
            signatureInstance.initVerify(certificate);
            signatureInstance.update(input.getBytes(StandardCharsets.UTF_8));
            return signatureInstance.verify(Base64.decode(signature));
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Error verifying signature of algorithm " + algorithm, e);
            throw new GeneralSecurityException(e);
        }
    }

    @Override
    public CipherText encrypt(Certificate certificate, String encryptionAlgorithm, String input)
            throws GeneralSecurityException {
        Cipher cipher = getCipherInstance(encryptionAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, certificate);
        byte[] cipherText = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return CipherText.builder()
                .algorithm(encryptionAlgorithm)
                .cipherText(Base64.toBase64String(cipherText))
                .build();
    }

    @Override
    public String decrypt(PrivateKey privateKey, CipherText cipherText)
            throws GeneralSecurityException {
        Cipher cipher = getCipherInstance(cipherText.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] originalText = cipher.doFinal(Base64.decode(cipherText.getCipherText().getBytes(StandardCharsets.UTF_8)));
        return new String(originalText, StandardCharsets.UTF_8);
    }

    @Override
    public CipherText encrypt(SecretKey secretKey, String encryptionAlgorithm, String input) throws GeneralSecurityException {
        final byte[] iv = CryptographyService.randomIv(secretKey.getEncoded().length);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = getCipherInstance(encryptionAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] cipherText = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return CipherText.builder()
                .algorithm(encryptionAlgorithm)
                .iv(Base64.toBase64String(iv))
                .cipherText(Base64.toBase64String(cipherText))
                .build();
    }

    @Override
    public String decrypt(SecretKey secretKey, CipherText cipherText) throws GeneralSecurityException {
        final byte[] iv = Base64.decode(cipherText.getIv());
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = getCipherInstance(cipherText.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] originalText = cipher.doFinal(Base64.decode(cipherText.getCipherText().getBytes(StandardCharsets.UTF_8)));
        return new String(originalText, StandardCharsets.UTF_8);
    }

    private Signature getSignatureInstance(String algorithm) throws NoSuchAlgorithmException {
        return Signature.getInstance(algorithm, BC_PROVIDER);
    }

    private Cipher getCipherInstance(String algorithm)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        String effectiveAlgorithm = encryptionAlgorithmMapping.get(algorithm);
        if (effectiveAlgorithm == null) {
            throw new NoSuchAlgorithmException(algorithm + " is not supported");
        }
        return Cipher.getInstance(effectiveAlgorithm, BC_PROVIDER);
    }

}
