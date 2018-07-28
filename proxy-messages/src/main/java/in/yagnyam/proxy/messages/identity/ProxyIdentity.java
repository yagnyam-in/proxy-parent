package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;

/**
 * Proxy for a Person
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"issuerId", "proxyId"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProxyIdentity implements SignableMessage, AddressableMessage {

    @NonNull
    private String issuerId;

    @NonNull
    private String proxyId;

    private String nationality;

    private String name;

    private String gender;

    private Integer age;

    private Boolean is18Plus;

    private Date dateOfBirth;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @Override
    public String signer() {
        return issuerId;
    }

    @Override
    public String toReadableString() {
        // TODO: Add optional fields
        return issuerId +
                " certify that " +
                proxyId +
                " is a valid id from " +
                creationDate +
                " till " +
                expiryDate +
                ".";
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(issuerId)
                && StringUtils.isValid(proxyId)
                && !DateUtils.isValid(creationDate)
                && !DateUtils.isValid(expiryDate);
        // TODO: Add optional fields
    }

    @Override
    public String address() {
        return proxyId;
    }
}
