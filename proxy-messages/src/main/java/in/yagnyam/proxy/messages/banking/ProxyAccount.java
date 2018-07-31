package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Proxy Account
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"proxyAccountId", "proxyId"})
public class ProxyAccount implements SignableMessage, AddressableMessage {

  @NonNull
  private ProxyAccountId proxyAccountId;

  @NonNull
  private ProxyId proxyId;

  @NonNull
  private Date creationDate;

  @NonNull
  private Date expiryDate;

  /**
   * Maximum amount for which *each* Payment can be made
   */
  @NonNull
  private Amount maximumAmountPerTransaction;

  @Override
  public ProxyId signer() {
    return ProxyId.of(proxyAccountId.getBankId());
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return proxyAccountId != null
        && proxyAccountId.isValid()
        && proxyId != null && proxyId.isValid()
        && DateUtils.isValid(creationDate)
        && DateUtils.isValid(expiryDate)
        && maximumAmountPerTransaction != null
        && maximumAmountPerTransaction.isValid();
  }

  @Override
  public ProxyId address() {
    return proxyId;
  }
}
