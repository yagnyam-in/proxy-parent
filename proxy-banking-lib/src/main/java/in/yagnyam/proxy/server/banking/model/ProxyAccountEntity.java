package in.yagnyam.proxy.server.banking.model;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import java.util.Date;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
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

  @Index
  @NonNull
  private String originalAccountId;

  @NonNull
  private ProxyId proxyId;

  private String accountName;

  @NonNull
  private Date creationDate;

  @NonNull
  private Date expiryDate;

  @NonNull
  private String currency;

  /**
   * Maximum amount for which *each* Payment can be made
   */
  @NonNull
  private Amount maximumAmountPerTransaction;

  @Load
  @Setter(AccessLevel.PROTECTED)
  private Ref<OriginalAccountEntity> originalAccountEntityRef;

  @Ignore
  @NonNull
  private OriginalAccountEntity originalAccountEntity;

  @OnLoad
  public void fetchOriginalAccountEntity() {
    if (originalAccountEntityRef != null) {
      originalAccountEntity = originalAccountEntityRef.get();
      originalAccountId = originalAccountEntityRef.getKey().getName();
    }
  }


  @OnSave
  public void setOriginalAccountEntityRef() {
    if (this.originalAccountEntity != null) {
      this.originalAccountEntityRef = Ref.create(originalAccountEntity);
      this.originalAccountId = originalAccountEntity.getAccountId();
    } else {
      this.originalAccountEntityRef = null;
    }
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

  public Amount getBalance() {
    return originalAccountEntity != null ? originalAccountEntity.getBalance() : null;
  }

}
