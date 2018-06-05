package in.yagnyam.proxy.banking.services;

import in.yagnyam.proxy.banking.db.ProxyAccountRepository;
import in.yagnyam.proxy.banking.model.ProxyAccountEntity;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Builder
public class ProxyAccountService {

    @NonNull
    private ProxyAccountRepository proxyAccountRepository;

    /**
     * Fetch proxy account for given Proxy Account Id
     * @param proxyAccountId Account Id
     * @return Proxy Account associated with Id
     */
    public Optional<ProxyAccountEntity> getProxyAccount(String proxyAccountId) {
        return proxyAccountRepository.fetchProxyAccount(proxyAccountId);
    }

}
