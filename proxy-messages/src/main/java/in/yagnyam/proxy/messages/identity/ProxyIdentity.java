package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import java.util.Date;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Proxy for a Person
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"issuerId", "ownerProxyId"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonRootName("in.yagnyam.proxy.messages.identity.ProxyIdentity")
public class ProxyIdentity implements SignableMessage, AddressableMessage {

  @NonNull
  private ProxyId issuerId;

  @NonNull
  private ProxyId proxyId;

  private String verificationMethod;

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
  public ProxyId signer() {
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
    return issuerId != null && issuerId.isValid()
        && proxyId != null && proxyId.isValid()
        && !DateUtils.isValid(creationDate)
        && !DateUtils.isValid(expiryDate);
    // TODO: Add optional fields
  }

  @Override
  public ProxyId address() {
    return proxyId;
  }
}
