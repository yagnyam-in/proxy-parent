package in.yagnyam.proxy.banking.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.banking.model.ProxyAccountEntity;
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
   * @param proxyAccount RequestEntity
   */
  public void saveProxyAccount(@NonNull ProxyAccountEntity proxyAccount) {
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().transact(() -> ofy().save().entity(proxyAccount).now());
      }
    });
  }


}
