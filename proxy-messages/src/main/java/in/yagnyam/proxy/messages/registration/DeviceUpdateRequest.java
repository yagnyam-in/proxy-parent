package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.Constants;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceUpdateRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private ProxyId proxyId;

    @NonNull
    private String deviceId;

    @NonNull
    private String fcmToken;

    private String deviceName;

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
        return StringUtils.isValid(requestId)
                && proxyId != null && proxyId.isValid()
                && StringUtils.isValid(deviceId)
                && StringUtils.isValid(fcmToken);
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId address() {
        return ProxyId.of(Constants.PROXY_CENTRAL);
    }

}
