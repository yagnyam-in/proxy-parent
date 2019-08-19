package in.yagnyam.proxy.messages.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyBaseObject;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeleteAlertsRequest implements SignableRequestMessage, AddressableMessage {

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AlertId implements ProxyBaseObject {

        @NonNull
        private String proxyUniverse;

        @NonNull
        private String alertType;

        @NonNull
        private String alertId;

        @Override
        public boolean isValid() {
            return StringUtils.isValid(proxyUniverse) && StringUtils.isValid(alertId) && StringUtils.isValid(alertType);
        }
    }

    @NonNull
    private String requestId;

    @NonNull
    private ProxyId proxyId;

    @NonNull
    private String deviceId;

    @Singular
    private List<AlertId> alertIds;

    // TODO: Make this mandatory
    private ProxyId alertProviderProxyId;

    @Override
    public ProxyId signer() {
        return proxyId;
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(proxyId)
                && StringUtils.isValid(requestId)
                && StringUtils.isValid(deviceId)
                && alertIds.stream().allMatch(AlertId::isValid);
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId address() {
        return alertProviderProxyId;
    }
}
