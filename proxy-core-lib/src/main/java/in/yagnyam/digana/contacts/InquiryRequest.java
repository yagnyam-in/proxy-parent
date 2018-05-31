package in.yagnyam.digana.contacts;

import lombok.*;

import java.util.Map;

/**
 * Customer Inquiry Request
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "requestNumber")
public class InquiryRequest {

    @NonNull
    private String requestNumber;

    @Singular
    @NonNull
    private Map<String, String> phoneNumbers;

    @Singular
    @NonNull
    private Map<String, String> emailAddresses;

}
