package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payee {

    @NonNull
    private String paymentEncashmentId;

    @NonNull
    private PayeeTypeEnum payeeType;

    private ProxyId proxyId;

    private String emailHash;

    private String phoneHash;

    private String secretHash;

    @JsonIgnore
    boolean isValid() {
        if (payeeType == null || StringUtils.isEmpty(paymentEncashmentId)) {
            return false;
        }
        switch (payeeType) {
            case ProxyId:
                return proxyId != null && proxyId.isValid();
            case Email:
                return StringUtils.isValid(emailHash) && StringUtils.isValid(secretHash);
            case Phone:
                return StringUtils.isValid(phoneHash) && StringUtils.isValid(secretHash);
            case AnyoneWithSecret:
                return StringUtils.isValid(secretHash);
            default:
                return false;
        }
    }

}
