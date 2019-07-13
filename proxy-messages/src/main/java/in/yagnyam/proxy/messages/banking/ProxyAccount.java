package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import lombok.*;

import java.util.Date;

/**
 * Proxy Account
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class ProxyAccount implements SignableMessage {

  @NonNull
  private ProxyAccountId proxyAccountId;

  @NonNull
  private ProxyId ownerProxyId;

  @NonNull
  private Date creationDate;

  @NonNull
  private Date expiryDate;

  @NonNull
  private String currency;

  /**
   * Maximum amount for which *each* PaymentAuthorization can be made
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
        && ownerProxyId != null && ownerProxyId.isValid()
        && DateUtils.isValid(creationDate)
        && DateUtils.isValid(expiryDate)
        && currency != null
        && maximumAmountPerTransaction != null && maximumAmountPerTransaction.isValid();
  }

  @JsonIgnore
  public String getBankId() {
    return proxyAccountId.getBankId();
  }

  @JsonIgnore
  public String getProxyUniverse() {
    return proxyAccountId.getProxyUniverse();
  }
}
