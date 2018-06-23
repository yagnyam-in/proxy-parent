package in.yagnyam.proxy.services;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * PEM Services for encoding and decoding Cryptographic objects to/from String
 */
public class PemService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    private String asPemString(String name, byte[] content) throws IOException {
        PemObject pemObject = new PemObject(name, content);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        JcaPEMWriter writer = new JcaPEMWriter(new PrintWriter(byteStream));
        writer.writeObject(pemObject);
        writer.close();
        return new String(byteStream.toByteArray(), "UTF-8");
    }

    private byte[] getPemContent(String encodedPemObject) throws IOException {
        try (PemReader pemReader = new PemReader(new InputStreamReader(new ByteArrayInputStream(encodedPemObject.getBytes("UTF-8"))))) {
            return pemReader.readPemObject().getContent();
        }
    }

    public PublicKey decodePublicKey(String algorithm, String encodedKey) throws GeneralSecurityException, IOException {
        KeyFactory factory = KeyFactory.getInstance(algorithm, PROVIDER_NAME);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(getPemContent(encodedKey));
        return factory.generatePublic(publicKeySpec);
    }

    public String encodePublicKey(PublicKey publicKey) throws IOException {
        return encodePublicKey(publicKey, "RSA PUBLIC KEY");
    }

    public String encodePublicKey(PublicKey publicKey, String name) throws IOException {
        return asPemString(name, publicKey.getEncoded());
    }


    public PrivateKey decodePrivateKey(String algorithm, String encodedKey) throws GeneralSecurityException, IOException {
        KeyFactory factory = KeyFactory.getInstance(algorithm, PROVIDER_NAME);
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(getPemContent(encodedKey));
        return factory.generatePrivate(privKeySpec);
    }

    public String encodePrivateKey(PrivateKey privateKey) throws IOException {
        return asPemString("RSA PRIVATE KEY", privateKey.getEncoded());
    }

    public X509Certificate decodeCertificate(String encodedCertificate) throws GeneralSecurityException, IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(getPemContent(encodedCertificate));
        CertificateFactory factory = CertificateFactory.getInstance("X.509", PROVIDER_NAME);
        return (X509Certificate) factory.generateCertificate(byteStream);
    }

    public String encodeCertificate(X509Certificate certificate) throws GeneralSecurityException, IOException {
        return asPemString("CERTIFICATE", certificate.getEncoded());
    }

    public String encodeCertificate(X509CertificateHolder certificateHolder) throws IOException {
        return asPemString("CERTIFICATE", certificateHolder.getEncoded());
    }

    public PKCS10CertificationRequest decodeCertificateRequest(final String encodedCertificateRequest) throws IOException {
        return new PKCS10CertificationRequest(getPemContent(encodedCertificateRequest));
    }

    /**
     * Returns Certificate Request as PEM encoded String
     * @param certificateRequest Certificate Request
     * @return PEM encoded string for Certificate Request
     * @throws IOException Any error while encoding
     */
    public String encodeCertificateRequest(final PKCS10CertificationRequest certificateRequest) throws IOException {
        return asPemString("CERTIFICATE REQUEST", certificateRequest.getEncoded());
    }

}
