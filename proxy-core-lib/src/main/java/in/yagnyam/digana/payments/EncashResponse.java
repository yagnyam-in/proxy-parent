package in.yagnyam.digana.payments;


import lombok.*;

@Data
@EqualsAndHashCode(of="requestNumber")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class EncashResponse {

    @NonNull
    private String requestNumber;

    private boolean success;

    private String transactionReference;

    private String errorCode;

    private String errorMessage;

    public static EncashResponse success(String requestNumber, String transactionReference) {
        return builder().requestNumber(requestNumber).success(true).transactionReference(transactionReference).build();
    }

    public static EncashResponse failure(String requestNumber, String errorMessage) {
        return builder().requestNumber(requestNumber).success(false).errorCode("UNKNOWN").errorMessage(errorMessage).build();
    }

}
