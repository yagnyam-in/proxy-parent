package in.yagnyam.proxy.server.banking.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.server.banking.model.OriginalAccountEntity;
import in.yagnyam.proxy.server.banking.model.ProxyAccountEntity;
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
  }

  /**
   * Fetch Proxy Account associated with Id
   *
   * @param proxyAccountId Proxy Account Id
   * @return Proxy Account associated with given Id
   */
  public Optional<ProxyAccountEntity> fetchProxyAccount(@NonNull String proxyAccountId) {
    return ObjectifyService.run(() -> Optional
        .ofNullable(ofy().load().key(Key.create(ProxyAccountEntity.class, proxyAccountId)).now()));
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
          proxyAccount.setOriginalAccountEntity(underlyingAccount);
          ofy().save().entity(proxyAccount).now();
        });
      }
    });
  }


}
