package in.yagnyam.proxy.server.banking.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.server.banking.model.AccountCredentialsEntity;
import in.yagnyam.proxy.server.banking.model.RepresentativeAccountEntity;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;

/**
 * Representative Account Repository for storing and retrieving from Database
 */
@Builder
public class RepresentativeAccountRepository {

  static {
    ObjectifyService.register(AccountCredentialsEntity.class);
    ObjectifyService.register(RepresentativeAccountEntity.class);
  }

  /**
   * Fetch all Representative Accounts
   *
   * @return All Representative Accounts
   */
  public List<RepresentativeAccountEntity> fetchAllRepresentativeAccounts() {
    return ObjectifyService.run(() -> ofy().load().type(RepresentativeAccountEntity.class).list());
  }

  /**
   * Fetch Representative Account matching given accountId
   *
   * @param accountId Account Id
   * @return Representative Account with given bank Id
   */
  public Optional<RepresentativeAccountEntity> getRepresentativeAccount(@NonNull String accountId) {
    return ObjectifyService.run(() ->
        Optional
            .ofNullable(
                ofy().load().key(Key.create(RepresentativeAccountEntity.class, accountId)).now())
    );
  }


  /**
   * Save the Representative Account to Database
   *
   * @param accountEntity Representative Account
   */
  public void saveRepresentativeAccount(@NonNull RepresentativeAccountEntity accountEntity) {
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().save().entity(accountEntity).now();
      }
    });
  }


  /**
   * Deposit the given amount to Account
   *
   * @param accountEntity Representative Account
   * @param amount Amount to deposit
   */
  public RepresentativeAccountEntity deposit(@NonNull RepresentativeAccountEntity accountEntity,
      @NonNull Amount amount) {
    return ObjectifyService.run(() -> ofy().transact(() -> {
          RepresentativeAccountEntity latest = ofy().load()
              .key(Key.create(RepresentativeAccountEntity.class, accountEntity.getAccountId())).now();
          latest.setBalance(latest.getBalance().add(amount));
          ofy().save().entity(latest);
          return latest;
        })
    );
  }


}
