package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
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
    public String address() {
        return proxyId;
    }
}
