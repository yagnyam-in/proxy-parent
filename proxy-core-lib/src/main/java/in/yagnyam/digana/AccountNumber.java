package in.yagnyam.digana;

import lombok.*;

/**
 * Account Number class to represent an Account
 */

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor(staticName = "of")
public class AccountNumber {

    @NonNull
    private String bankNumber;

    @NonNull
    private String accountNumber;

}
