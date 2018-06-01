package in.yagnyam.proxy.messages.payments;

import lombok.*;

/**
 * Account Number class to represent an Account
 */

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor(staticName = "of")
public class ProxyAccountId {

    @NonNull
    private String bankId;

    @NonNull
    private String accountId;

}
