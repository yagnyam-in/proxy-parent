package in.yagnyam.digana.server;

import in.yagnyam.digana.server.model.Bank;
import in.yagnyam.digana.server.model.CertificateEntity;
import in.yagnyam.proxy.Certificate;

import java.util.Date;

public final class TestUtils {

    public static Certificate sampleCertificate(String serialNumber) {
        return Certificate.builder()
                .serialNumber(serialNumber)
                .owner("OWNER")
                .sha256Thumbprint("SHA256")
                .validFrom(new Date())
                .validTill(new Date())
                .certificateEncoded("CERT")
                .subject("SUB")
                .build();
    }

    public static Certificate sampleCertificateForFingerPrint(String fingerPrint) {
        return Certificate.builder()
                .serialNumber("dummy")
                .sha256Thumbprint(fingerPrint)
                .owner("OWNER")
                .validFrom(new Date())
                .validTill(new Date())
                .certificateEncoded("CERT")
                .subject("SUB")
                .build();
    }

    public static Bank sampleBank(String id) {
        return Bank.builder().bankNumber(id).name("Sample").build();
    }
}
