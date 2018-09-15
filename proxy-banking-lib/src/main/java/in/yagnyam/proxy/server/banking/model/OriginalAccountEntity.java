package in.yagnyam.proxy.server.banking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import in.yagnyam.proxy.messages.banking.Amount;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;

/**
 * Entity to represent Original Account
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "accountId")
public class OriginalAccountEntity {

  @Id
  @NonNull
  private String accountId;

  @Index
  @NonNull
  private String accountNumber;

  @NonNull
  private String bank;

  @NonNull
  @Singular
  private List<String> accountHolders;

  @NonNull
  private String currency;

  @NonNull
  @Setter
  private Amount balance;

  @Load
  @Setter(AccessLevel.PROTECTED)
  @JsonIgnore
  private Ref<AccountCredentialsEntity> credentialsEntityRef;

  @Ignore
  @JsonIgnore
  private AccountCredentialsEntity credentialsEntity;

  @OnLoad
  public void fetchCredentialsEntity() {
    if (credentialsEntityRef != null) {
      credentialsEntity = credentialsEntityRef.get();
    }
  }

  @OnSave
  public void setCredentialsEntityRef() {
    if (this.credentialsEntity != null) {
      this.credentialsEntityRef = Ref.create(credentialsEntity);
    } else {
      this.credentialsEntityRef = null;
    }
  }

}
