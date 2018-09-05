package in.yagnyam.proxy.server.banking.model;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.Currency;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Entity to represent Proxy Account
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "proxyAccountId")
public class ProxyAccountEntity {

  @Id
  @NonNull
  private String proxyAccountId;

  @NonNull
  private String bankId;

  @NonNull
  private ProxyId proxyId;

  private String accountName;

  @NonNull
  private Date creationDate;

  @NonNull
  private Date expiryDate;

  @NonNull
  private Currency currency;

  /**
   * Maximum amount for which *each* Payment can be made
   */
  @NonNull
  private Amount maximumAmountPerTransaction;

  @Load
  private Ref<OriginalAccountEntity> originalAccountEntityRef;

  public OriginalAccountEntity getOriginalAccountEntity() {
    return originalAccountEntityRef.get();
  }

  public void setOriginalAccountEntity(OriginalAccountEntity originalAccountEntity) {
    this.originalAccountEntityRef = Ref.create(originalAccountEntity);
  }

  public ProxyAccountId asProxyAccountId() {
    return ProxyAccountId.builder()
        .accountId(proxyAccountId)
        .bankId(bankId)
        .build();
  }

  public ProxyAccount asProxyAccount() {
    return ProxyAccount.builder()
        .proxyAccountId(asProxyAccountId())
        .proxyId(proxyId)
        .currency(currency)
        .maximumAmountPerTransaction(maximumAmountPerTransaction)
        .creationDate(creationDate)
        .expiryDate(expiryDate)
        .build();
  }

}
