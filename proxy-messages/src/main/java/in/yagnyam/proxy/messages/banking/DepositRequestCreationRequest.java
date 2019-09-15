package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;


/**
 * Request to deposit money to Underlying Proxy Account
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"depositId"})
public class DepositRequestCreationRequest implements SignableRequestMessage, AddressableMessage {

    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @ToString
    public static class RequestingCustomer {

        private String name;

        private String phone;

        private String email;

        public boolean isValid() {
            return StringUtils.isValid(name) && StringUtils.isValid(phone) && StringUtils.isValid(email);
        }

    }

    @NonNull
    private String depositId;

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private Amount amount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NonNull
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RequestingCustomer requestingCustomer;

    @Override
    public ProxyId address() {
        return proxyAccount.getSignedBy();
    }

    @Override
    public String requestId() {
        return depositId;
    }

    @Override
    public ProxyId signer() {
        return getOwnerProxyId();
    }@Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(depositId)
                && proxyAccount != null && proxyAccount.isValid()
                && StringUtils.isValid(message)
                && amount != null && amount.isValid()
                && (requestingCustomer == null || requestingCustomer.isValid());
    }

    @JsonIgnore
    public ProxyAccountId getProxyAccountId() {
        return proxyAccount.getMessage().getProxyAccountId();
    }

    @JsonIgnore
    public ProxyId getOwnerProxyId() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return getProxyAccountId().getProxyUniverse();
    }
}
