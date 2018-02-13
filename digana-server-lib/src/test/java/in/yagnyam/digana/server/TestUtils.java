package in.yagnyam.digana.server;

import in.yagnyam.digana.server.model.Bank;
import in.yagnyam.digana.server.model.Certificate;

public final class TestUtils {

    public static Certificate sampleCertificate(String serialNumber) {
        return Certificate.builder()
                .serialNumber(serialNumber)
                .owner("OWNER")
                .build();
    }

    public static Certificate sampleCertificateForFingerPrint(String fingerPrint) {
        return Certificate.builder()
                .serialNumber("dummy")
                .sha256Thumbprint(fingerPrint)
                .owner("OWNER")
                .build();
    }

    public static Bank sampleBank(String id) {
        return Bank.builder().bankNumber(id).name("Sample").build();
    }
}
