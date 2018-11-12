package in.yagnyam.proxy.server.banking.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.server.banking.model.AccountCredentialsEntity;
import in.yagnyam.proxy.server.banking.model.BankConfigurationEntity;
import in.yagnyam.proxy.server.banking.model.RepresentativeAccountEntity;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;

/**
 * Bank Configuration Repository for storing and retrieving from Database
 */
@Builder
public class BankConfigurationRepository {

  static {
    ObjectifyService.register(BankConfigurationEntity.class);
    ObjectifyService.register(AccountCredentialsEntity.class);
    ObjectifyService.register(RepresentativeAccountEntity.class);
  }

  /**
   * Fetch all Bank Configurations
   *
   * @return All Bank Configurations
   */
  public List<BankConfigurationEntity> fetchAllBankConfigurations() {
    return ObjectifyService.run(() -> ofy().load().type(BankConfigurationEntity.class).list());
  }

  /**
   * Fetch all Bank Configurations for given Currency
   * @param currency Currency
   * @return All Bank Configurations for given Currency
   */
  public List<BankConfigurationEntity> fetchBankConfigurationsForCurrency(String currency) {
    return ObjectifyService.run(() -> ofy().load().type(BankConfigurationEntity.class).filter("currency", currency).list());
  }


  /**
   * Fetch Bank configuration matching given bankId
   *
   * @param bankId Bank Id
   * @return Bank Configuration with given bank Id
   */
  public Optional<BankConfigurationEntity> getBankConfiguration(@NonNull String bankId) {
    return ObjectifyService.run(() ->
        Optional
            .ofNullable(ofy().load().key(Key.create(BankConfigurationEntity.class, bankId)).now())
    );
  }


  /**
   * Save the Bank Configuration to Database
   *
   * @param bankConfiguration Bank Configuration
   */
  public void saveBankConfiguration(@NonNull BankConfigurationEntity bankConfiguration) {
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().save().entity(bankConfiguration).now();
      }
    });
  }


}
