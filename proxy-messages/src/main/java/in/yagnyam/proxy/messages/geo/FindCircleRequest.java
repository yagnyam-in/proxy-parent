package in.yagnyam.proxy.messages.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;


@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FindCircleRequest implements SignableRequestMessage {

    @NonNull
    private String requestId;

    @NonNull
    private ProxyId serviceProviderProxyId;

    @NonNull
    private ProxyId requesterProxyId;

    @NonNull
    private GeoPoint geoLocation;

    @Override
    public ProxyId signer() {
        return requesterProxyId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && ProxyUtils.isValid(serviceProviderProxyId)
                && ProxyUtils.isValid(requesterProxyId)
                && ProxyUtils.isValid(geoLocation);
    }

    @Override
    public String requestId() {
        return requestId;
    }

}
