package in.yagnyam.digana.services;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.jose4j.base64url.Base64Url;
import org.jose4j.lang.HashUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;

/**
 * Cryptography Service
 */
@Slf4j
@NoArgsConstructor(staticName = "instance")
public class CryptographyService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String KEY_GENERATION_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";
    private static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;
    private static final int KEY_SIZE = 2048;

    public int getKeySize() {
        return KEY_SIZE;
    }

    public String getProviderName() {
        return PROVIDER_NAME;
    }

    public String getKeyGenerationAlgorithm() {
        return KEY_GENERATION_ALGORITHM;
    }

    public String sha1Thumbprint(X509CertificateHolder certificate) throws IOException {
        return base64urlThumbprint(certificate, "SHA-1");
    }

    public String sha256Thumbprint(X509CertificateHolder certificate) throws IOException {
        return base64urlThumbprint(certificate, "SHA-256");
    }

    private static String base64urlThumbprint(X509CertificateHolder certificate, String hashAlg) throws IOException {
        MessageDigest msgDigest = HashUtil.getMessageDigest(hashAlg);
        byte[] certificateEncoded = certificate.getEncoded();
        byte[] digest = msgDigest.digest(certificateEncoded);
        return Base64Url.encode(digest);
    }


    private static SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws IOException {
        ASN1InputStream is = null;
        try {
            is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()));
            ASN1Sequence seq = (ASN1Sequence) is.readObject();
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
            return createSubjectKeyIdentifier(info);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static SubjectKeyIdentifier createSubjectKeyIdentifier(SubjectPublicKeyInfo info) throws IOException {
        return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
    }


    private static X509CertificateHolder buildCertificate(X509v3CertificateBuilder certificateBuilder, PrivateKey signedWithPrivateKey)
            throws OperatorCreationException, GeneralSecurityException {
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(PROVIDER_NAME).build(signedWithPrivateKey);
        return certificateBuilder.build(signer);
    }


    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM, PROVIDER_NAME);
        generator.initialize(KEY_SIZE, new SecureRandom());
        return generator.generateKeyPair();
    }


    public String getSignature(byte[] chequeBytes, String algorithm, PrivateKey privateKey) {
        try {
            Signature signature = getSignatureInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(chequeBytes);
            return Base64.toBase64String(signature.sign());
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifySignature(byte[] chequeBytes, String algorithm, Certificate certificate, String signatureBytes) {
        try {
            Signature signature = getSignatureInstance(algorithm);
            signature.initVerify(certificate);
            signature.update(chequeBytes);
            return signature.verify(Base64.decode(signatureBytes));
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private Signature getSignatureInstance(String algorithm) throws NoSuchProviderException, NoSuchAlgorithmException {
        return Signature.getInstance(algorithm, PROVIDER_NAME);
    }


}
