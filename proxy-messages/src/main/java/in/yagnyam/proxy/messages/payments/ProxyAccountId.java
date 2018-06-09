package in.yagnyam.proxy.messages.payments;

import lombok.*;

/**
 * Account Number class to represent an Account
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProxyAccountId {

    @NonNull
    private String bankId;

    @NonNull
    private String accountId;

}
