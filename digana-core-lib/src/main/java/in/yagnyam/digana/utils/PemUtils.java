package in.yagnyam.digana.utils;

import lombok.NonNull;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class PemUtils {

    public static final String KEY_GENERATION_ALGORITHM = "RSA";
    public static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    public static String asPemString(@NonNull String name, @NonNull byte[] content) throws IOException {
        PemObject pemObject = new PemObject(name, content);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        JcaPEMWriter writer = new JcaPEMWriter(new PrintWriter(byteStream));
        writer.writeObject(pemObject);
        // Don't use try with resources here
        writer.close();
        return new String(byteStream.toByteArray(), "UTF-8");
    }

    public static byte[] getPemContent(@NonNull String encodedPemObject) throws IOException {
        try (PemReader pemReader = new PemReader(new InputStreamReader(new ByteArrayInputStream(encodedPemObject.getBytes("UTF-8"))))) {
            return pemReader.readPemObject().getContent();
        } catch (Throwable t) {
            throw new IOException("Invalid Pem Content", t);
        }
    }

    public static byte[] getPemContent(@NonNull InputStream inputStream) throws IOException {
        try (PemReader pemReader = new PemReader(new InputStreamReader(inputStream))) {
            return pemReader.readPemObject().getContent();
        } catch (Throwable t) {
            throw new IOException("Invalid Pem Content", t);
        }
    }


    public static PublicKey decodePublicKey(@NonNull String encodedKey) throws GeneralSecurityException, IOException {
        KeyFactory factory = KeyFactory.getInstance(KEY_GENERATION_ALGORITHM, PROVIDER_NAME);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(getPemContent(encodedKey));
        return factory.generatePublic(publicKeySpec);
    }

    public static String encodePublicKey(@NonNull PublicKey publicKey) throws GeneralSecurityException, IOException {
        return asPemString("RSA PUBLIC KEY", publicKey.getEncoded());
    }

    public static PrivateKey decodePrivateKey(@NonNull String encodedKey) throws GeneralSecurityException, IOException {
        KeyFactory factory = KeyFactory.getInstance(KEY_GENERATION_ALGORITHM, PROVIDER_NAME);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(getPemContent(encodedKey));
        return factory.generatePrivate(privateKeySpec);
    }

    public static String encodePrivateKey(@NonNull PrivateKey privateKey) throws GeneralSecurityException, IOException {
        return asPemString("RSA PRIVATE KEY", privateKey.getEncoded());
    }

    public static X509Certificate decodeCertificate(@NonNull String encodedCertificate) throws GeneralSecurityException, IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(getPemContent(encodedCertificate));
        CertificateFactory factory = CertificateFactory.getInstance("X.509", PROVIDER_NAME);
        return (X509Certificate) factory.generateCertificate(byteStream);
    }

    public static String encodeCertificate(@NonNull X509Certificate certificate) throws GeneralSecurityException, IOException {
        return asPemString("CERTIFICATE", certificate.getEncoded());
    }

    public static String encodeCertificate(@NonNull X509CertificateHolder certificateHolder) throws GeneralSecurityException, IOException {
        return asPemString("CERTIFICATE", certificateHolder.getEncoded());
    }

    public static PKCS10CertificationRequest decodeCertificateRequest(@NonNull String encodedCertificateRequest) throws GeneralSecurityException, IOException {
        return new PKCS10CertificationRequest(getPemContent(encodedCertificateRequest));
    }

    public static String encodeCertificateRequest(@NonNull PKCS10CertificationRequest certificateRequest) throws GeneralSecurityException, IOException {
        return asPemString("CERTIFICATE REQUEST", certificateRequest.getEncoded());
    }

    public static X509Certificate decodeCertificate(@NonNull InputStream inputStream) throws GeneralSecurityException, IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(getPemContent(inputStream));
        CertificateFactory factory = CertificateFactory.getInstance("X.509", PROVIDER_NAME);
        return (X509Certificate) factory.generateCertificate(byteStream);
    }


}
