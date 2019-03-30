package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ProxyAccountId {

  @NonNull
  private String proxyUniverse;

  @NonNull
  private String bankId;

  @NonNull
  private String accountId;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(proxyUniverse)
        && StringUtils.isValid(bankId)
        && StringUtils
        .isValid(accountId);
  }
}
