package in.yagnyam.proxy.server.banking.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import in.yagnyam.proxy.messages.banking.Amount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity to represent Representative Account (Customer Facing Account)
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Cache
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class RepresentativeAccountEntity {

  @Id
  private String accountId;

  @Index
  private String accountNumber;

  private String bank;

  private String accountHolder;

  private String currency;

  private Amount balance;

  public String getBalanceString() {
    if (balance != null) {
      return balance.getValue() + " " + balance.getCurrency();
    } else {
      return "0";
    }
  }

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
