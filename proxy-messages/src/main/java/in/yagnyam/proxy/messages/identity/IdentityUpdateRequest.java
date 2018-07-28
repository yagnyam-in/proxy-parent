package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Request Message to Create/Update Identity (Subject) details
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class IdentityUpdateRequest implements SignableRequestMessage {

    /**
     * Unique Request Id
     */
    @NonNull
    private String requestId;

    /**
     * Identity Subject that is being modified
     */
    @NonNull
    private IdentitySubject subject;

    /**
     * Owner of this Identity.
     * <p>
     * Only owner is allowed to Update the identity details
     */
    @NonNull
    private String ownerProxyId;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public String signer() {
        return ownerProxyId;
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && StringUtils.isValid(ownerProxyId)
                && subject != null && subject.isValid();
    }
}
