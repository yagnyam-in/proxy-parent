package in.yagnyam.digana;

import lombok.*;

/**
 * Account class to represent an Account
 */

@EqualsAndHashCode(of="accountNumber")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@Getter
@ToString
public class Account {

    @NonNull
    private AccountNumber accountNumber;

    @NonNull
    private Amount balance;

    public static Account of(AccountNumber accountNumber, Amount balance) {
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = balance;
        return account;
    }
}
