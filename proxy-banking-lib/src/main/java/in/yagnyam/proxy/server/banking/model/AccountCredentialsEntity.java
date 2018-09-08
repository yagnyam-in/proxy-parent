package in.yagnyam.proxy.server.banking.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity to store credentials to access real account from the Bank. Extend this class to add fields
 * necessary to interact with Bank.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "credentialId")
public class AccountCredentialsEntity {

  @Id
  @NonNull
  protected String credentialId;

  public static AccountCredentialsEntityBuilder accountCredentialsEntityBuilder() {
    return new AccountCredentialsEntityBuilder();
  }

  public static class AccountCredentialsEntityBuilder extends
      AbstractAccountCredentialsEntityBuilder<AccountCredentialsEntity, AccountCredentialsEntityBuilder> {

    @Override
    public AccountCredentialsEntity newInstance() {
      return new AccountCredentialsEntity();
    }
  }

  /**
   * As AccountCredentialsEntity will always be extended, this would make builder pattern for
   * subclasses easier to construct
   */
  @SuppressWarnings("unchecked")
  public static abstract class AbstractAccountCredentialsEntityBuilder<T extends AccountCredentialsEntity, B extends AbstractAccountCredentialsEntityBuilder> {

    String credentialId;

    protected static void assertNotNull(@NonNull String field, Object value) {
      if (value == null) {
        throw new NullPointerException(field + " is null");
      }
    }

    public B credentialId(String credentialId) {
      this.credentialId = credentialId;
      return thisInstance();
    }

    protected T populate(T accountCredentialsEntity) {
      assertNotNull("credentialId", credentialId);
      accountCredentialsEntity.credentialId = credentialId;
      return accountCredentialsEntity;
    }


    protected B thisInstance() {
      return (B) this;
    }

    public T build() {
      return populate(newInstance());
    }

    protected abstract T newInstance();

  }
}
