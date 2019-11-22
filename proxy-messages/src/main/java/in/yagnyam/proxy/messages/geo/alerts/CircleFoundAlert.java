package in.yagnyam.proxy.messages.geo.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableAlertMessage;
import in.yagnyam.proxy.messages.geo.FindCircleSessionId;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CircleFoundAlert implements SignableAlertMessage {

    @NonNull
    private String alertId;

    @NonNull
    private ProxyId receiverId;

    @NonNull
    private FindCircleSessionId sessionId;

    @NonNull
    private String proxyUniverse;

    @Override
    public ProxyId signer() {
        return sessionId.getServiceProviderId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(alertId)
                && ProxyUtils.isValid(receiverId)
                && ProxyUtils.isValid(sessionId)
                && StringUtils.isValid(proxyUniverse);
    }

    @Override
    public String proxyUniverse() {
        return proxyUniverse;
    }

    @Override
    public String alertId() {
        return alertId;
    }

    @Override
    public List<ProxyId> receivers() {
        return Collections.singletonList(receiverId);
    }
}
