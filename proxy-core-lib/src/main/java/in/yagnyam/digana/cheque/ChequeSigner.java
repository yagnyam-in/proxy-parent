package in.yagnyam.digana.cheque;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface ChequeSigner {

    String getVersion();

    void signChequeBook(ChequeBook chequeBook, PrivateKey bankKey, X509Certificate bankCertificate) throws IOException;

    void signCheque(Cheque cheque, PrivateKey customerKey, X509Certificate customerCertificate) throws IOException;

    boolean verifyChequeBookSignature(ChequeBook chequeBook, X509Certificate bankCertificate) throws IOException;

    boolean verifyChequeSignature(Cheque cheque, X509Certificate customerCertificate) throws IOException;
}
