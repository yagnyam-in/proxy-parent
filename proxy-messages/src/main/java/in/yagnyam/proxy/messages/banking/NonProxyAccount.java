package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Regular Bank Account/Wallets for Payments (Not a proxy account)
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
    @NonNull
    private String bank;

    /**
     * Account Number
     */
    @NonNull
    private String accountNumber;

    /**
     * Account Holder Name
     */
    @NonNull
    private String accountHolder;

    /**
     * Currency
     */
    @NonNull
    private String currency;

    /**
     * IFSC Code of the Account. Only applicable in India
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String ifscCode;

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(bank)
                && StringUtils.isValid(accountNumber)
                && StringUtils.isValid(accountHolder)
                && Currency.isValidCurrency(currency)
                && (ifscCode == null || StringUtils.isValid(ifscCode));
    }

}
