package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyBaseObject;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.utils.ProxyUtils;
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
 * Account Number class to represent an Account
 */

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class ProxyAccountId implements ProxyBaseObject {

  @NonNull
  private String proxyUniverse;

  @NonNull
  private ProxyId bankProxyId;

  @NonNull
  private String accountId;

  @JsonIgnore
  @Override
  public boolean isValid() {
    return StringUtils.isValid(proxyUniverse)
        && ProxyUtils.isValid(bankProxyId)
        && StringUtils
        .isValid(accountId);
  }

  @JsonIgnore
  public String getBankId() {
    return bankProxyId.getId();
  }
}
