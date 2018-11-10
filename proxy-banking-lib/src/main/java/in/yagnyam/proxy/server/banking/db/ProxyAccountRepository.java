package in.yagnyam.proxy.server.banking.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.server.banking.model.AccountCredentialsEntity;
import in.yagnyam.proxy.server.banking.model.OriginalAccountEntity;
import in.yagnyam.proxy.server.banking.model.ProxyAccountEntity;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;

/**
 * Repository for storing and retrieving Proxy Accounts from DB
 */
@Builder
public class ProxyAccountRepository {

  static {
    ObjectifyService.register(ProxyAccountEntity.class);
    ObjectifyService.register(OriginalAccountEntity.class);
    ObjectifyService.register(AccountCredentialsEntity.class);
  }

  /**
   * Fetch Proxy Account associated with Id
   *
   * @param proxyAccountId Proxy Account Id
   * @return Proxy Account associated with given Id
   */
  public Optional<ProxyAccountEntity> fetchProxyAccount(@NonNull String proxyAccountId) {
    return ObjectifyService.run(() -> {
      ProxyAccountEntity result = ofy().load()
          .key(Key.create(ProxyAccountEntity.class, proxyAccountId))
          .now();
      return Optional.ofNullable(result);
    });
  }

  /**
   * Fetch Proxy Account associated with Id
   *
   * @param proxyAccountId Proxy Account Id
   * @return Proxy Account associated with given Id
   */
  public Optional<ProxyAccountEntity> fetchProxyAccount(@NonNull ProxyAccountId proxyAccountId) {
    return ObjectifyService.run(() -> {
      ProxyAccountEntity result = ofy().load()
          .key(Key.create(ProxyAccountEntity.class, proxyAccountId.getAccountId()))
          .now();
      return Optional.ofNullable(result)
          .filter(a -> StringUtils.equals(a.getBankId(), proxyAccountId.getBankId()));
    });
  }

  /**
   * Fetch Linked Proxy Accounts associated with given Original/Underlying Account Id
   *
   * @param originalAccountId Original/Underlying Account Id
   * @return Proxy Accounts associated with given original account id
   */
  public List<ProxyAccountEntity> fetchProxyAccountsByOriginalAccountId(@NonNull String originalAccountId) {
    return ObjectifyService.run(() -> ofy().load()
        .type(ProxyAccountEntity.class)
        .filter("originalAccountId", originalAccountId)
        .list()
    );
  }


  /**
   * Save the Proxy Account to Database
   *
   * @param proxyAccount Proxy Account
   */
  public void saveProxyAccount(@NonNull ProxyAccountEntity proxyAccount) {
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().transact(() -> ofy().save().entity(proxyAccount).now());
      }
    });
  }

  /**
   * Save the Proxy Account and Underlying Account to Database
   *
   * @param proxyAccount Proxy Account
   * @param underlyingAccount Underlying Account
   */
  public void saveProxyAccountWithLinkedAccount(@NonNull ProxyAccountEntity proxyAccount,
      @NonNull OriginalAccountEntity underlyingAccount) {
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().transact(() -> {
          ofy().save().entity(underlyingAccount).now();
          ofy().save().entity(proxyAccount).now();
        });
      }
    });
  }


  /**
   * Save the Proxy Account and Underlying Account to Database
   *
   * @param proxyAccount Proxy Account
   * @param underlyingAccount Underlying Account
   * @param credentials Account Credentials
   */
  public void saveProxyAccountWithLinkedAccountAndCredentials(
      @NonNull ProxyAccountEntity proxyAccount,
      @NonNull OriginalAccountEntity underlyingAccount,
      @NonNull AccountCredentialsEntity credentials) {
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().transact(() -> {
          ofy().save().entity(underlyingAccount).now();
          ofy().save().entity(proxyAccount).now();
          ofy().save().entity(credentials).now();
        });
      }
    });
  }


}
