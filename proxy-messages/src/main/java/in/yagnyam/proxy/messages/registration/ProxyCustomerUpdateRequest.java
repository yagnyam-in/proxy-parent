package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Request to update Customer details for given Proxy Id
 */
@Getter
@ToString
@EqualsAndHashCode(of = "requestId", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProxyCustomerUpdateRequest implements SignableRequestMessage {

    @NonNull
    private String requestId;

    @NonNull
    private ProxyId proxyId;

    private String gcmToken;

    private String name;

    private String emailAddress;

    private String phoneNumber;

    private boolean syncWithContacts;

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
                && proxyId != null && proxyId.isValid();
    }

    @Override
    public String requestId() {
        return requestId;
    }
}
