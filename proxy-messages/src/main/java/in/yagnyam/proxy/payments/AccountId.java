package in.yagnyam.proxy.payments;

import lombok.*;

/**
 * Account Number class to represent an Account
 */

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor(staticName = "of")
public class AccountId {

    @NonNull
    private String bankId;

    @NonNull
    private String accountId;

}
