package in.yagnyam.digana.cheque;

import in.yagnyam.digana.authorization.AccountAuthorizationRequest;

import java.io.IOException;

/**
 * Class to serialize {@link Cheque Cheque} and {@link ChequeBook ChequeBook}
 *
 * @author Yagnyam
 * ChequeSignerFactory}
 */
public interface ChequeSerializer {

    String getVersion();

    byte[] serializeChequeBook(ChequeBook chequeBook) throws IOException;

    byte[] serializeCheque(Cheque cheque) throws IOException;

    byte[] serializeAccountAuthorizationRequest(AccountAuthorizationRequest authorizationRequest) throws IOException;
}
