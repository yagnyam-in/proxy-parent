package in.yagnyam.digana.registration;

import lombok.*;

import java.util.Map;

/**
 * Registration Response
 */
@Data
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class RegistrationResponse {

    @NonNull
    private String requestNumber;

    private boolean success;

    private String errorCode;

    private String errorMessage;

    private String certificateSerial;

    @Singular
    private Map<String, String> certificates;

    public static RegistrationResponse success(@NonNull String requestNumber, @NonNull String certificateSerial, @NonNull Map<String, String> certificates) {
        return RegistrationResponse.builder().requestNumber(requestNumber).success(true).certificateSerial(certificateSerial).certificates(certificates).build();
    }

    public static RegistrationResponse failure(@NonNull String requestNumber, String errorMessage) {
        return RegistrationResponse.builder().requestNumber(requestNumber).success(false).errorCode("UNKNOWN").errorMessage(errorMessage).build();
    }

    public static RegistrationResponse failure(@NonNull String requestNumber, String errorCode, String errorMessage) {
        return RegistrationResponse.builder().requestNumber(requestNumber).success(false).errorCode(errorCode).errorMessage(errorMessage).build();
    }

}
