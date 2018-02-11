package in.yagnyam.digana;

import in.yagnyam.digana.model.Bank;
import in.yagnyam.digana.model.Certificate;

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
