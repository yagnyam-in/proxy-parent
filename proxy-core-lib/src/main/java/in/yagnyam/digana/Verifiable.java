package in.yagnyam.digana;

public interface Verifiable {

    String getVersion();

    String getSignatureOneAlgorithm();

    String getSignatureTwoAlgorithm();

    String getSignatureOne();

    String getSignatureTwo();

    String getVerificationCertificateSerial();

    String getVerificationCertificateFingerPrintAlgorithm();

    String getVerificationCertificateFingerPrint();
}
