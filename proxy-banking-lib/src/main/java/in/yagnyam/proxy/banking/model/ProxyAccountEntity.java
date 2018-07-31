package in.yagnyam.proxy.banking.model;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import in.yagnyam.proxy.messages.banking.Amount;
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
  @Index
  private String proxyId;

  @Index
  @NonNull
  private String originalAccountId;

  @NonNull
  private Date creationDate;

  @NonNull
  private Date expiryDate;

  /**
   * Maximum amount for which *each* Payment can be made
   */
  @NonNull
  private Amount maximumAmountPerTransaction;

  @Load
  private Ref<OriginalAccountEntity> originalAccountEntityRef;

  @Load
  private Ref<AccountCredentialsEntity> accountCredentialsEntityRef;

  public OriginalAccountEntity getOriginalAccountEntity() {
    return originalAccountEntityRef.get();
  }

  public void setOriginalAccountEntity(OriginalAccountEntity originalAccountEntity) {
    this.originalAccountEntityRef = Ref.create(originalAccountEntity);
  }

  public AccountCredentialsEntity getAccountCredentialsEntity() {
    return accountCredentialsEntityRef.get();
  }

  public void setAccountCredentialsEntity(AccountCredentialsEntity accountCredentialsEntity) {
    this.accountCredentialsEntityRef = Ref.create(accountCredentialsEntity);
  }
}
