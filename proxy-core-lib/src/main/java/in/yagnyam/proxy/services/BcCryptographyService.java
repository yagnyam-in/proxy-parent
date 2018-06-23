package in.yagnyam.proxy.services;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.encoders.Base64;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.Certificate;

/**
 * Cryptography Service using Bouncy Castle
 */
@Slf4j
@Builder
public class BcCryptographyService extends CryptographyService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    @NonNull
    private PemService pemService;

    public KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(getKeyGenerationAlgorithm(), PROVIDER_NAME);
        generator.initialize(getKeySize(), new SecureRandom());
        return generator.generateKeyPair();
    }


    @Override
    public String createCertificateRequest(KeyPair keyPair, String signatureAlgorithm, X500Principal subject) throws GeneralSecurityException {
        try {
            PKCS10CertificationRequest certificationRequest = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic())
                    .build(new JcaContentSignerBuilder(signatureAlgorithm).setProvider(PROVIDER_NAME).build(keyPair.getPrivate()));
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
        } catch (UnsupportedEncodingException e) {
            log.error("Error signing for algorithm " + algorithm, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verifySignature(String input, String algorithm, Certificate certificate, String signature) throws GeneralSecurityException {
        try {
            Signature signatureInstance = getSignatureInstance(algorithm);
            signatureInstance.initVerify(certificate);
            signatureInstance.update(input.getBytes(DEFAULT_CHARSET));
            return signatureInstance.verify(Base64.decode(signature.getBytes(DEFAULT_CHARSET)));
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Error verifying signature of algorithm " + algorithm, e);
            throw new GeneralSecurityException(e);
        } catch (UnsupportedEncodingException e) {
            log.error("Error verifying signature of algorithm " + algorithm, e);
            throw new RuntimeException(e);
        }
    }

    private Signature getSignatureInstance(String algorithm) throws NoSuchProviderException, NoSuchAlgorithmException {
        return Signature.getInstance(algorithm, PROVIDER_NAME);
    }

}
