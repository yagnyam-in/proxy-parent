package in.yagnyam.digana.contacts;

import lombok.*;

import java.util.Map;

/**
 * Customer Inquiry response
 */
@Data
@EqualsAndHashCode(of = "requestNumber")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class InquiryResponse {

    @NonNull
    private String requestNumber;

    private boolean success;

    private String errorCode;

    private String errorMessage;

    @Singular
    @NonNull
    private Map<String, String> phoneNumbers;

    @Singular
    @NonNull
    private Map<String, String> emailAddresses;
}
