package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Payee
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payee {

    @NonNull
    private PayeeTypeEnum payeeType;

    private ProxyAccountId proxyAccountId;

    private ProxyId proxyId;

    private String email;

    private String phone;

    private String ivPrefixedSecretHash;

    /**
     * Tests if the message is valid
     *
     * @return true if message is valid, false otherwise
     */
    @JsonIgnore
    boolean isValid() {
        if (payeeType == null) {
            return false;
        }
        switch (payeeType) {
            case ProxyAccountId:
                return proxyAccountId != null && proxyAccountId.isValid() && proxyId != null && proxyId.isValid();
            case ProxyId:
                return proxyId != null && proxyId.isValid();
            case Email:
                return StringUtils.isValid(email) && StringUtils.isValid(ivPrefixedSecretHash);
            case Phone:
                return StringUtils.isValid(phone) && StringUtils.isValid(ivPrefixedSecretHash);
            case AnyoneWithSecret:
                return StringUtils.isValid(ivPrefixedSecretHash);
            default:
                return false;
        }
    }

}
