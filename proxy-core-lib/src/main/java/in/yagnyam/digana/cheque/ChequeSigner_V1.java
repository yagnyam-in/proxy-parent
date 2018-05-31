package in.yagnyam.digana.cheque;

import in.yagnyam.proxy.services.CryptographyService;
import lombok.Builder;
import lombok.NonNull;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Builder
public class ChequeSigner_V1 implements ChequeSigner {

    public static final String VERSION = "V1";
    public static final String DS_ALGORITHM_ONE = "SHA256WithRSA";
    public static final String DS_ALGORITHM_TWO = "MD5withRSA";

    @NonNull
    private final CryptographyService cryptographyService;

    @NonNull
    private final ChequeSerializer serializer;

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void signChequeBook(ChequeBook chequeBook, PrivateKey bankKey, X509Certificate bankCertificate) throws IOException {
        byte[] chequeBytes = serializer.serializeChequeBook(chequeBook);
        chequeBook.setBankSignatureOne(cryptographyService.getSignature(chequeBytes, DS_ALGORITHM_ONE, bankKey));
        chequeBook.setBankSignatureTwo(cryptographyService.getSignature(chequeBytes, DS_ALGORITHM_TWO, bankKey));
        chequeBook.setBankCertificateSerial(bankCertificate.getSerialNumber().toString());
    }


    @Override
    public boolean verifyChequeBookSignature(@NonNull ChequeBook chequeBook, @NonNull X509Certificate bankCertificate) throws IOException {
        byte[] chequeBytes = serializer.serializeChequeBook(chequeBook);
        return cryptographyService.verifySignature(chequeBytes, DS_ALGORITHM_ONE, bankCertificate, chequeBook.getBankSignatureOne())
                && cryptographyService.verifySignature(chequeBytes, DS_ALGORITHM_TWO, bankCertificate, chequeBook.getBankSignatureTwo());
    }

    @Override
    public void signCheque(@NonNull Cheque cheque, @NonNull PrivateKey customerKey, @NonNull X509Certificate customerCertificate) throws IOException {
        byte[] chequeBytes = serializer.serializeCheque(cheque);
        cheque.setPayerSignatureOne(cryptographyService.getSignature(chequeBytes, DS_ALGORITHM_ONE, customerKey));
        cheque.setPayerSignatureTwo(cryptographyService.getSignature(chequeBytes, DS_ALGORITHM_TWO, customerKey));
        cheque.setPayerCertificateSerial(customerCertificate.getSerialNumber().toString());
    }

    @Override
    public boolean verifyChequeSignature(@NonNull Cheque cheque, @NonNull X509Certificate customerCertificate) throws IOException {
        if (!verifyChequeBookSignature(cheque.getChequeBook(), customerCertificate)) {
            return false;
        }
        byte[] chequeBytes = serializer.serializeCheque(cheque);
        return cryptographyService.verifySignature(chequeBytes, DS_ALGORITHM_ONE, customerCertificate, cheque.getPayerSignatureOne())
                && cryptographyService.verifySignature(chequeBytes, DS_ALGORITHM_TWO, customerCertificate, cheque.getPayerSignatureTwo());
    }

}
