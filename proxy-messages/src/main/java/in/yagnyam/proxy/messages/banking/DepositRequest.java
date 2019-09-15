package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.StringUtils;
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
 * Deposit Request
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"depositId", "proxyAccount"})
public class DepositRequest implements SignableMessage {

  @NonNull
  private String depositId;

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;

  @NonNull
  private Amount amount;

  @NonNull
  private Date creationDate;

  @NonNull
  private Date expiryDate;

  @NonNull
  private String depositLink;

  @Override
  public ProxyId signer() {
    return getProxyAccountId().getBankProxyId();
  }@Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(depositId)
        && proxyAccount != null && proxyAccount.isValid()
        && amount != null && amount.isValid()
        && DateUtils.isValid(creationDate)
        && DateUtils.isValid(expiryDate)
        && StringUtils.isValid(depositLink);
  }

  @JsonIgnore
  public ProxyId getOwnerProxyId() {
    return proxyAccount.getMessage().getOwnerProxyId();
  }

  @JsonIgnore
  public ProxyAccountId getProxyAccountId() {
    return proxyAccount.getMessage().getProxyAccountId();
  }

  @JsonIgnore
  public String getProxyUniverse() {
    return getProxyAccountId().getProxyUniverse();
  }
}
