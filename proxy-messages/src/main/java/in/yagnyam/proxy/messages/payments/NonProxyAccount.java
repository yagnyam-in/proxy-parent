package in.yagnyam.proxy.messages.payments;

import lombok.*;

/**
 * Regular Account for Payments (Not a proxy account)
 */
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(of = {"bank", "accountNumber"})
public class NonProxyAccount {

    /**
     * Bank Reference
     */
    private String bank;

    /**
     * Account Number
     */
    private String accountNumber;

    /**
     * Account Holder Name
     */
    private String accountHolder;

}
