package in.yagnyam.proxy.messages.geo;

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
public class FindCircleSessionId implements ProxyBaseObject {

    @NonNull
    private ProxyId serviceProviderId;

    @NonNull
    private String sessionId;

    @JsonIgnore
    @Override
    public boolean isValid() {
        return ProxyUtils.isValid(serviceProviderId)
                && StringUtils.isValid(sessionId);
    }

}
