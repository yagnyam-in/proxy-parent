package in.yagnyam.proxy.server.banking.services;

import in.yagnyam.proxy.server.banking.db.ProxyAccountRepository;
import in.yagnyam.proxy.server.banking.model.OriginalAccountEntity;
import in.yagnyam.proxy.server.banking.model.ProxyAccountEntity;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ProxyAccountService {

  @NonNull
  private ProxyAccountRepository proxyAccountRepository;

  /**
   * Fetch proxy account for given Proxy Account Id
   *
   * @param proxyAccountId Account Id
   * @return Proxy Account associated with Id
   */
  public Optional<ProxyAccountEntity> getProxyAccount(String proxyAccountId) {
    return proxyAccountRepository.fetchProxyAccount(proxyAccountId);
  }

  public void saveProxyAccount(@NonNull ProxyAccountEntity proxyAccount,
      @NonNull OriginalAccountEntity underlyingAccount) {
    proxyAccountRepository.saveProxyAccountWithLinkedAccount(proxyAccount, underlyingAccount);
  }

}
