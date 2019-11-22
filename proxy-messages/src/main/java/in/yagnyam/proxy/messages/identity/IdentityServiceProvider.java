package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.CollectionUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IdentityServiceProvider implements SignableMessage {

    @NonNull
    private ProxyId proxyId;

    @NonNull
    private String proxyUniverse;

    @NonNull
    private List<SubjectIdTypeEnum> supportedSubjectTypes;

    @NonNull
    private String apiEndpoint;

    @NonNull
    private String name;

    @Override
    public ProxyId signer() {
        return ProxyId.of("proxy-identity");
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(proxyId)
                && StringUtils.isValid(proxyUniverse)
                && CollectionUtils.isNotEmpty(supportedSubjectTypes)
                && StringUtils.isValid(apiEndpoint)
                && StringUtils.isValid(name);
    }

    @JsonIgnore
    public String getId() {
        return proxyId.getId();
    }

}
