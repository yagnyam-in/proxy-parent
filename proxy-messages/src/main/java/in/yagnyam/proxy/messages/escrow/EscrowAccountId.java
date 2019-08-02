package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyBaseObject;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountId implements ProxyBaseObject {

    @NonNull
    private String proxyUniverse;

    @NonNull
    private ProxyId bankProxyId;

    @NonNull
    private String accountId;

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(proxyUniverse)
                && ProxyUtils.isValid(bankProxyId)
                && StringUtils.isValid(accountId);
    }
}
