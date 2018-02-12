package in.yagnyam.digana.authorization;

import in.yagnyam.digana.AbstractVerifiable;
import lombok.*;

/**
 * Account Authorization Request
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
public class AccountAuthorizationRequest extends AbstractVerifiable {

    @NonNull
    private String requestNumber;

    @NonNull
    private String customerNumber;

    @NonNull
    private String bankNumber;

    private String bankCustomerNumber;

    private String bankAccountNumber;

    private String login;

    private String password;

    private String authorizationToken;

    @Builder
    private AccountAuthorizationRequest(
            @NonNull String version,
            @NonNull String verificationCertificateSerial,
            @NonNull String verificationCertificateFingerPrintAlgorithm,
            @NonNull String verificationCertificateFingerPrint,
            @NonNull String signatureOneAlgorithm,
            @NonNull String signatureTwoAlgorithm,
            String signatureOne,
            String signatureTwo,
            @NonNull String requestNumber,
            @NonNull String customerNumber,
            @NonNull String bankNumber,
            String bankCustomerNumber,
            String bankAccountNumber,
            String login,
            String password,
            String authorizationToken) {
        super(
                version,
                verificationCertificateSerial,
                verificationCertificateFingerPrintAlgorithm,
                verificationCertificateFingerPrint,
                signatureOneAlgorithm,
                signatureTwoAlgorithm,
                signatureOne,
                signatureTwo);
        this.requestNumber = requestNumber;
        this.customerNumber = customerNumber;
        this.bankNumber = bankNumber;
        this.bankCustomerNumber = bankCustomerNumber;
        this.bankAccountNumber = bankAccountNumber;
        this.login = login;
        this.password = password;
        this.authorizationToken = authorizationToken;
    }
}
