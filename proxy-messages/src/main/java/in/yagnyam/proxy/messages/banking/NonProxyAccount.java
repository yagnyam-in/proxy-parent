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

    /**
     * Sometimes, it is mandatory withdraw money to specify Mail Id
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String email;

    /**
     * Sometimes, it is mandatory withdraw money to specify Phone
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String phone;

    /**
     * Sometimes, it is mandatory withdraw money to specify Address
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String address;

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(bank)
                && StringUtils.isValid(accountNumber)
                && StringUtils.isValid(accountHolder)
                && Currency.isValidCurrency(currency)
                && (ifscCode == null || StringUtils.isValid(ifscCode))
                && (email == null || StringUtils.isValid(email))
                && (phone == null || StringUtils.isValid(phone))
                && (address == null || StringUtils.isValid(address));
    }

}
