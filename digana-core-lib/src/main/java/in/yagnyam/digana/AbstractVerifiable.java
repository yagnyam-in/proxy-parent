package in.yagnyam.digana;

public class AbstractVerifiable implements Verifiable {

  private String version;

  private String signatureOneAlgorithm;

  private String signatureTwoAlgorithm;

  private String signatureOne;

  private String signatureTwo;

  private String verificationCertificateSerial;

  private String verificationCertificateFingerPrintAlgorithm;

  private String verificationCertificateFingerPrint;

  protected AbstractVerifiable(
      String version,
      String verificationCertificateSerial,
      String verificationCertificateFingerPrintAlgorithm,
      String verificationCertificateFingerPrint,
      String signatureOneAlgorithm,
      String signatureTwoAlgorithm,
      String signatureOne,
      String signatureTwo) {
    this.version = version;
    this.verificationCertificateSerial = verificationCertificateSerial;
    this.verificationCertificateFingerPrintAlgorithm = verificationCertificateFingerPrintAlgorithm;
    this.verificationCertificateFingerPrint = verificationCertificateFingerPrint;
    this.signatureOneAlgorithm = signatureOneAlgorithm;
    this.signatureTwoAlgorithm = signatureTwoAlgorithm;
    this.signatureOne = signatureOne;
    this.signatureTwo = signatureTwo;
  }

  protected AbstractVerifiable() {
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getSignatureOneAlgorithm() {
    return signatureOneAlgorithm;
  }

  @Override
  public String getSignatureTwoAlgorithm() {
    return signatureTwoAlgorithm;
  }

  @Override
  public String getSignatureOne() {
    return signatureOne;
  }

  @Override
  public String getSignatureTwo() {
    return signatureTwo;
  }

  @Override
  public String getVerificationCertificateSerial() {
    return verificationCertificateSerial;
  }

  @Override
  public String getVerificationCertificateFingerPrintAlgorithm() {
    return verificationCertificateFingerPrintAlgorithm;
  }

  @Override
  public String getVerificationCertificateFingerPrint() {
    return verificationCertificateFingerPrint;
  }

  public void setSignatureOneAlgorithm(String signatureOneAlgorithm) {
    this.signatureOneAlgorithm = signatureOneAlgorithm;
  }

  public void setSignatureTwoAlgorithm(String signatureTwoAlgorithm) {
    this.signatureTwoAlgorithm = signatureTwoAlgorithm;
  }

  public void setSignatureOne(String signatureOne) {
    this.signatureOne = signatureOne;
  }

  public void setSignatureTwo(String signatureTwo) {
    this.signatureTwo = signatureTwo;
  }

  public void setVerificationCertificateSerial(String verificationCertificateSerial) {
    this.verificationCertificateSerial = verificationCertificateSerial;
  }

  public void setVerificationCertificateFingerPrintAlgorithm(
      String verificationCertificateFingerPrintAlgorithm) {
    this.verificationCertificateFingerPrintAlgorithm = verificationCertificateFingerPrintAlgorithm;
  }

  public void setVerificationCertificateFingerPrint(String verificationCertificateFingerPrint) {
    this.verificationCertificateFingerPrint = verificationCertificateFingerPrint;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
