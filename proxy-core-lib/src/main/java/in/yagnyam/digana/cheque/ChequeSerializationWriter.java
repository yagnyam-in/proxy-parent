package in.yagnyam.digana.cheque;

import in.yagnyam.digana.authorization.AccountAuthorizationRequest;

import java.io.IOException;

/**
 * Class to serialize {@link Cheque Cheque} and {@link ChequeBook ChequeBook}
 *
 * @author Yagnyam
 * ChequeSignerFactory}
 */
public interface ChequeSerializationWriter {

    String getVersion();

    void serializeChequeBook(SerializationStream serializationStream, ChequeBook chequeBook) throws IOException;

    void serializeCheque(SerializationStream serializationStream, Cheque cheque) throws IOException;

    void serializeAccountAuthorizationRequest(SerializationStream serializationStream, AccountAuthorizationRequest authorizationRequest) throws IOException;
}
