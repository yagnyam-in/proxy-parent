package in.yagnyam.digana.customers;

import lombok.*;

/**
 * Customer Update Response
 */
@Data
@EqualsAndHashCode(of="requestNumber")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class CustomerUpdateResponse {

    @NonNull
    private String requestNumber;

    private boolean success;

    private String errorCode;

    private String errorMessage;

    public static CustomerUpdateResponse success(@NonNull String requestNumber) {
        return CustomerUpdateResponse.builder().requestNumber(requestNumber).success(true).build();
    }

    public static CustomerUpdateResponse failure(@NonNull String requestNumber, String errorMessage) {
        return CustomerUpdateResponse.builder().requestNumber(requestNumber).success(false).errorCode("UNKNOWN").errorMessage(errorMessage).build();
    }

    public static CustomerUpdateResponse failure(@NonNull String requestNumber, String errorCode, String errorMessage) {
        return CustomerUpdateResponse.builder().requestNumber(requestNumber).success(false).errorCode(errorCode).errorMessage(errorMessage).build();
    }

}
