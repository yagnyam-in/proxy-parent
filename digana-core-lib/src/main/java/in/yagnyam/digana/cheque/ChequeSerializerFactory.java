package in.yagnyam.digana.cheque;

import in.yagnyam.digana.authorization.AccountAuthorizationRequest;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Cheque Serializer Factory to serialize any Cheque
 */
@NoArgsConstructor(staticName = "instance")
public class ChequeSerializerFactory {

    private static final Map<String, ChequeSerializer> supportedSerializers = initSerializers();

    private static Map<String, ChequeSerializer> initSerializers() {
        return Collections.singletonMap(ChequeSerializer_V1.VERSION, ChequeSerializer_V1.create());
    }

    public byte[] serializeChequeBook(@NonNull ChequeBook chequeBook) throws IOException {
        if (isEmpty(chequeBook.getVersionNumber())) {
            throw new IllegalArgumentException();
        }
        ChequeSerializer serializer = supportedSerializers.get(chequeBook.getVersionNumber());
        if (serializer == null) {
            throw new IllegalArgumentException("non supported cheque book version - " + chequeBook.getVersionNumber());
        }
        return serializer.serializeChequeBook(chequeBook);
    }

    public byte[] serializeCheque(@NonNull Cheque cheque) throws IOException {
        if (isEmpty(cheque.getVersionNumber())) {
            throw new IllegalArgumentException();
        }
        ChequeSerializer serializer = supportedSerializers.get(cheque.getVersionNumber());
        if (serializer == null) {
            throw new IllegalArgumentException("non supported cheque version - " + cheque.getVersionNumber());
        }
        return serializer.serializeCheque(cheque);
    }


    public byte[] serializeAccountAuthorizationRequest(@NonNull AccountAuthorizationRequest authorizationRequest) throws IOException {
        if (isEmpty(authorizationRequest.getVersion())) {
            throw new IllegalArgumentException();
        }
        ChequeSerializer serializer = supportedSerializers.get(authorizationRequest.getVersion());
        if (serializer == null) {
            throw new IllegalArgumentException("non supported serializer for version - " + authorizationRequest.getVersion());
        }
        return serializer.serializeAccountAuthorizationRequest(authorizationRequest);
    }

    private static boolean isEmpty(String versionNumber) {
        return versionNumber == null || versionNumber.trim().isEmpty();
    }
}
