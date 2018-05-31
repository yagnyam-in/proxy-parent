package in.yagnyam.digana.cheque;

import in.yagnyam.digana.services.CryptographyService;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Class to sign and verify {@link Cheque Cheque}
 *
 * @author Yagnyam
 * @see {@link Cheque Cheque}
 */
public class ChequeSignerFactory {

    public static ChequeSigner getInstance(String version) {
        if (ChequeSigner_V1.VERSION.equals(version)) {
            return ChequeSigner_V1.builder()
                    .cryptographyService(CryptographyService.instance())
                    .serializer(ChequeSerializer_V1.create())
                    .build();
        }
        throw new IllegalArgumentException("Invalid Cheque version " + version);
    }


    public void signChequeBook(ChequeBook chequeBook, PrivateKey bankKey, X509Certificate bankCertificate) throws IOException {
        if (chequeBook == null || bankKey == null || chequeBook.getVersionNumber() == null) {
            throw new IllegalArgumentException();
        }
        ChequeSigner signer = getInstance(chequeBook.getVersionNumber());
        signer.signChequeBook(chequeBook, bankKey, bankCertificate);
    }

    public void signCheque(Cheque cheque, PrivateKey customerKey, X509Certificate customerCertificate) throws IOException {
        if (cheque == null || customerKey == null || cheque.getVersionNumber() == null) {
            throw new IllegalArgumentException();
        }
        ChequeSigner signer = getInstance(cheque.getVersionNumber());
        signer.signCheque(cheque, customerKey, customerCertificate);
    }

    public boolean verifyChequeBookSignature(ChequeBook chequeBook, X509Certificate bankCertificate) throws IOException {
        if (chequeBook == null || bankCertificate == null || chequeBook.getVersionNumber() == null) {
            throw new IllegalArgumentException();
        }
        ChequeSigner signer = getInstance(chequeBook.getVersionNumber());
        return signer.verifyChequeBookSignature(chequeBook, bankCertificate);
    }


    public boolean verifyChequeSignature(Cheque cheque, X509Certificate customerCertificate) throws IOException {
        if (cheque == null || customerCertificate == null || cheque.getVersionNumber() == null) {
            throw new IllegalArgumentException();
        }
        ChequeSigner signer = getInstance(cheque.getVersionNumber());
        return signer.verifyChequeSignature(cheque, customerCertificate);
    }

}
