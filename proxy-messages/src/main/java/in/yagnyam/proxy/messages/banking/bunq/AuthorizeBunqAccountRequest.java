package in.yagnyam.proxy.messages.banking.bunq;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.messages.banking.Currency;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;


@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizeBunqAccountRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private ProxyId ownerProxyId;

    @NonNull
    private ProxyId bankProxyId;

    @NonNull
    private String apiToken;

    private String accountNumber;

    @NonNull
    private String currency;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId signer() {
        return ownerProxyId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && ProxyUtils.isValid(ownerProxyId)
                && ProxyUtils.isValid(bankProxyId)
                && StringUtils.isValid(apiToken)
                && Currency.isValidCurrency(currency);
    }

    @Override
    public ProxyId address() {
        return bankProxyId;
    }
}
