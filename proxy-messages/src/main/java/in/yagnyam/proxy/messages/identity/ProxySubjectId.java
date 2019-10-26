package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyBaseObject;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Id to represent a Something like Person.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class ProxySubjectId implements ProxyBaseObject {

    @NonNull
    private String proxyUniverse;

    @NonNull
    private ProxyId issuerProxyId;

    @NonNull
    private String subjectId;

    @JsonIgnore
    @Override
    public boolean isValid() {
        return StringUtils.isValid(proxyUniverse)
                && ProxyUtils.isValid(issuerProxyId)
                && StringUtils.isValid(subjectId);
    }

    @JsonIgnore
    public String getIssuerId() {
        return issuerProxyId.getId();
    }
}
