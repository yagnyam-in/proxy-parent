package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Regular Account for Payments (Not a proxy account)
 */
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(of = {"bank", "accountNumber"})
public class NonProxyAccount {

  /**
   * Bank Reference
   */
  private String bank;

  /**
   * Account Number
   */
  private String accountNumber;

  /**
   * Account Holder Name
   */
  private String accountHolder;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(bank)
        && StringUtils.isValid(accountNumber)
        && StringUtils.isValid(accountHolder);
  }

}
