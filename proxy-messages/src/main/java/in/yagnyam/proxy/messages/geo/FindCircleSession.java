package in.yagnyam.proxy.messages.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FindCircleSession implements SignableMessage {

    @NonNull
    private FindCircleSessionId sessionId;

    @NonNull
    private ProxyId requesterProxyId;

    // As WebSockets are not yet supported, and FCM alerts are too slow, using Firestore location to send updates.
    // TODO: This should be removed.
    private String alertLocation;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @Override
    public ProxyId signer() {
        return sessionId.getServiceProviderId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(sessionId)
                && ProxyUtils.isValid(requesterProxyId)
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate);
    }

}
