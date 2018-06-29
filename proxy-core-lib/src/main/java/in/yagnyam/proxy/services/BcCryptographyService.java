package in.yagnyam.proxy.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.encoders.Base64;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Cryptography Service using Bouncy Castle
 */
@Slf4j
@Builder
public class BcCryptographyService extends CryptographyService {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();

    static {
        Security.addProvider(BC_PROVIDER);
    }

    @NonNull
    private PemService pemService;

    public KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(getKeyGenerationAlgorithm(), BC_PROVIDER);
        generator.initialize(getKeySize(), new SecureRandom());
        return generator.generateKeyPair();
    }

    @Override
    public String createCertificateRequest(KeyPair keyPair, String signatureAlgorithm, X500Principal subject)
            throws GeneralSecurityException {
        try {
            PKCS10CertificationRequest certificationRequest = new JcaPKCS10CertificationRequestBuilder(subject,
                    keyPair.getPublic())
                            .build(new JcaContentSignerBuilder(signatureAlgorithm).setProvider(BC_PROVIDER)
                                    .build(keyPair.getPrivate()));
            return pemService.encodeCertificateRequest(certificationRequest);
        } catch (OperatorCreationException | IOException e) {
            log.error("Error while creating certificate request for subject " + subject, e);
            throw new GeneralSecurityException("Error while creating certificate request", e);
        }
    }

    @Override
    public String getSignature(String input, String algorithm, PrivateKey privateKey) throws GeneralSecurityException {
        try {
            Signature signature = getSignatureInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(input.getBytes(DEFAULT_CHARSET));
            return Base64.toBase64String(signature.sign());
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Error signing for algorithm " + algorithm, e);
            throw new GeneralSecurityException(e);
        }
    }

    @Override
    public boolean verifySignature(String input, String algorithm, Certificate certificate, String signature)
            throws GeneralSecurityException {
        try {
            Signature signatureInstance = getSignatureInstance(algorithm);
            signatureInstance.initVerify(certificate);
            signatureInstance.update(input.getBytes(DEFAULT_CHARSET));
            return signatureInstance.verify(Base64.decode(signature.getBytes(DEFAULT_CHARSET)));
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Error verifying signature of algorithm " + algorithm, e);
            throw new GeneralSecurityException(e);
        }
    }

    private Signature getSignatureInstance(String algorithm) throws NoSuchProviderException, NoSuchAlgorithmException {
        return Signature.getInstance(algorithm, BC_PROVIDER);
    }

    @Override
    public String ecnrypt(String input, Certificate certificate) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION, BC_PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, certificate);
        byte[] cipherText = cipher.doFinal(input.getBytes(DEFAULT_CHARSET));
        return Base64.toBase64String(cipherText);
    }

    @Override
    public String decrypt(String cipherText, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION, BC_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] originalText = cipher.doFinal(Base64.decode(cipherText.getBytes(DEFAULT_CHARSET)));
        return new String(originalText, DEFAULT_CHARSET);
    }

}
