package in.yagnyam.proxy.server.banking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import in.yagnyam.proxy.Proxy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(of = {"bankId", "privateKeyId", "bankName", "representativeAccountId"})
@AllArgsConstructor
@NoArgsConstructor
@Cache
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(of = {"bankId", "privateKeyId", "bankName", "representativeAccountId"})
public class BankConfigurationEntity {

  @Id
  private String bankId;

  private String bankName;

  private String privateKeyId;

  private String representativeAccountId;

  @Ignore
  @JsonIgnore
  private Proxy proxy;

  @Ignore
  @JsonIgnore
  private RepresentativeAccountEntity representativeAccount;

  @Load
  @Setter(AccessLevel.PROTECTED)
  @JsonIgnore
  private Ref<RepresentativeAccountEntity> representativeAccountRef;


  @OnLoad
  @JsonIgnore
  public void fetchRepresentativeAccount() {
    if (representativeAccountRef != null) {
      representativeAccount = representativeAccountRef.get();
    }
  }


  @OnSave
  @JsonIgnore
  public void setRepresentativeAccountRef() {
    if (this.representativeAccount != null) {
      this.representativeAccountRef = Ref.create(representativeAccount);
    } else {
      this.representativeAccountRef = null;
    }
  }


}
