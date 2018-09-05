package in.yagnyam.proxy.server.banking.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Entity to store credentials to access real account from the Bank. Extend this class to add fields
 * necessary to interact with Bank.
 */
@Entity
@Getter
@ToString
@EqualsAndHashCode(of = "credentialId")
public class AccountCredentialsEntity {

  @Id
  @NonNull
  private String credentialId;

  public static AccountCredentialsEntityBuilder builder() {
    return new AccountCredentialsEntityBuilder();
  }

  /**
   * As AccountCredentialsEntity will always be extended, this would make builder pattern for
   * subclasses easier to construct
   */
  @SuppressWarnings("unchecked")
  public static class AccountCredentialsEntityBuilder<T extends AccountCredentialsEntityBuilder> {

    protected String credentialId;

    protected static void assertNotNull(String field, Object value) {
      if (value == null) {
        throw new NullPointerException(field + " is null");
      }
    }

    public T credentialId(String credentialId) {
      this.credentialId = credentialId;
      return thisInstance();
    }

    protected AccountCredentialsEntity populate(AccountCredentialsEntity accountCredentialsEntity) {
      assertNotNull("credentialId", credentialId);
      accountCredentialsEntity.credentialId = credentialId;
      return accountCredentialsEntity;
    }


    public T thisInstance() {
      return (T) this;
    }


    public AccountCredentialsEntity build() {
      return populate(new AccountCredentialsEntity());
    }
  }
}
