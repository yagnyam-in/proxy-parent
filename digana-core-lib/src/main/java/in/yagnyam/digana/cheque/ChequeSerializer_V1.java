package in.yagnyam.digana.cheque;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import in.yagnyam.digana.Verifiable;
import in.yagnyam.digana.authorization.AccountAuthorizationRequest;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;

/**
 * Class to serialize {@link Cheque Cheque} version v1
 *
 * @author Yagnyam
 * @see {@link ChequeSigner ChequeSigner} {@link ChequeSignerFactory ChequeSignerFactory}
 */
@NoArgsConstructor(staticName = "create")
public class ChequeSerializer_V1 implements ChequeSerializationWriter, ChequeSerializer {

    public static final String VERSION = "V1";

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public byte[] serializeChequeBook(ChequeBook chequeBook) throws IOException {
        SerializationStream serializationStream = DERSerializationStream.create();
        serializeChequeBook(serializationStream, chequeBook);
        return serializationStream.finish();
    }

    @Override
    public byte[] serializeCheque(Cheque cheque) throws IOException {
        SerializationStream serializationStream = DERSerializationStream.create();
        serializeCheque(serializationStream, cheque);
        return serializationStream.finish();
    }

    @Override
    public byte[] serializeAccountAuthorizationRequest(AccountAuthorizationRequest authorizationRequest) throws IOException {
        SerializationStream serializationStream = DERSerializationStream.create();
        serializeAccountAuthorizationRequest(serializationStream, authorizationRequest);
        return serializationStream.finish();
    }

    @Override
    public void serializeChequeBook(@NonNull SerializationStream serializationStream, @NonNull ChequeBook chequeBook) throws IOException {
        if (!chequeBook.getVersionNumber().equals(VERSION)) {
            throw new IllegalArgumentException("Invalid version " + chequeBook.getVersionNumber());
        }
        // Version
        serializationStream.writeVersion(getVersion());
        // Cheque Book Number
        serializationStream.writeNumber(chequeBook.getChequeBookNumber());
        // Cheque Leaves
        serializationStream.writeNumber(chequeBook.getInitialChequeNumber());
        serializationStream.writeNumber(chequeBook.getNumberOfCheques());
        // Institution Details
        writeInstitution(serializationStream, chequeBook.getBank());
        serializationStream.writeString(chequeBook.getBankCertificateSerial());
        // Cheque Owner / Payer Details
        writePayer(serializationStream, chequeBook.getPayer());
        // Don't include any details about Payer Except Payer Number
        // Maximum Amount
        writeAmount(serializationStream, chequeBook.getMaximumAmount());
        // Creation and Expiry
        serializationStream.writeDate(chequeBook.getCreationDate());
        serializationStream.writeDate(chequeBook.getExpiryDate());
    }

    @Override
    public void serializeCheque(@NonNull SerializationStream serializationStream, @NonNull Cheque cheque) throws IOException {
        serializeChequeBook(serializationStream, cheque.getChequeBook());
        // Cheque Number
        serializationStream.writeNumber(cheque.getChequeNumber());
        // Payer Certificate Details
        serializationStream.writeString(cheque.getPayerCertificateSerial());
        // Payee
        writePayee(serializationStream, cheque.getPayee());
        writePayeeAccount(serializationStream, cheque.getPayeeAccount());
        // Amount
        writeAmount(serializationStream, cheque.getAmount());
        // Dates
        serializationStream.writeDate(cheque.getIssueDate());
        serializationStream.writeDate(cheque.getValidFrom());
        serializationStream.writeDate(cheque.getValidTill());
        // Transaction
        writeTransaction(serializationStream, cheque.getTransaction());
    }

    @Override
    public void serializeAccountAuthorizationRequest(SerializationStream serializationStream, AccountAuthorizationRequest authorizationRequest) throws IOException {
        serializeVerifiable(serializationStream, authorizationRequest);
        serializationStream.writeString(authorizationRequest.getRequestNumber());
        serializationStream.writeString(authorizationRequest.getBankNumber());
        serializationStream.writeString(authorizationRequest.getBankCustomerNumber());
        serializationStream.writeString(authorizationRequest.getBankAccountNumber());
        serializationStream.writeString(authorizationRequest.getLogin());
        serializationStream.writeString(authorizationRequest.getPassword());
        serializationStream.writeString(authorizationRequest.getAuthorizationToken());
    }

    private void writeAmount(SerializationStream serializationStream, @NonNull Amount amount) throws IOException {
        serializationStream.writeString(amount.getCurrency());
        serializationStream.writeNumber(amount.getValue());
    }

    private void writePayer(SerializationStream serializationStream, @NonNull Payer payer) throws IOException {
        // Don't include any details about Payer Except Payer Number
        serializationStream.writeString(payer.getPayerNumber());
    }

    private void writePayeeAccount(SerializationStream serializationStream, @NonNull AccountNumber payeeAccount) throws IOException {
        serializationStream.writeString(payeeAccount.getBankNumber());
        serializationStream.writeString(payeeAccount.getAccountNumber());
    }


    private void writePayee(SerializationStream serializationStream, @NonNull Payee payee) throws IOException {
        // All Payee attributes to prevent fraud
        serializationStream.writeString(payee.getPayeeNumber());
        serializationStream.writeString(payee.getName());
        serializationStream.writeString(payee.getPhoneNumber());
        serializationStream.writeString(payee.getAddress());
    }

    private void writeInstitution(SerializationStream serializationStream, @NonNull Bank bank) throws IOException {
        serializationStream.writeString(bank.getBankNumber());
    }

    private void writeTransaction(SerializationStream serializationStream, @NonNull ChequeTransaction transaction) throws IOException {
        serializationStream.writeString(transaction.getTransactionNumber());
        serializationStream.writeString(transaction.getShortDescription());
        serializationStream.writeString(transaction.getTransactionURL());
    }

    protected void serializeVerifiable(SerializationStream serializationStream, Verifiable verifiable) throws IOException {
        serializationStream.writeVersion(verifiable.getVersion());
        serializationStream.writeString(verifiable.getVerificationCertificateSerial());
        serializationStream.writeString(verifiable.getVerificationCertificateFingerPrintAlgorithm());
        serializationStream.writeString(verifiable.getVerificationCertificateFingerPrint());
        serializationStream.writeString(verifiable.getSignatureOneAlgorithm());
        serializationStream.writeString(verifiable.getSignatureTwoAlgorithm());
    }


}
