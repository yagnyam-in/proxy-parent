package in.yagnyam.digana.utils;

import lombok.NonNull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class PemUtils {

    public static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;


    public static byte[] getPemContent(@NonNull InputStream inputStream) throws IOException {
        try (PemReader pemReader = new PemReader(new InputStreamReader(inputStream))) {
            return pemReader.readPemObject().getContent();
        } catch (Throwable t) {
            throw new IOException("Invalid Pem Content", t);
        }
    }

    public static X509Certificate decodeCertificate(@NonNull InputStream inputStream) throws GeneralSecurityException, IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(getPemContent(inputStream));
        CertificateFactory factory = CertificateFactory.getInstance("X.509", PROVIDER_NAME);
        return (X509Certificate) factory.generateCertificate(byteStream);
    }


}
