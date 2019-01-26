package in.yagnyam.proxy.server.banking.services;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.server.ServiceException;
import in.yagnyam.proxy.server.banking.db.BankConfigurationRepository;
import in.yagnyam.proxy.server.banking.db.RepresentativeAccountRepository;
import in.yagnyam.proxy.server.banking.model.BankConfigurationEntity;
import in.yagnyam.proxy.server.model.PrivateKeyEntity;
import in.yagnyam.proxy.server.services.PrivateKeyService;
import in.yagnyam.proxy.services.CertificateService;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Bank Configuration service
 */
@Builder
@Getter
@Slf4j
public class BankConfigurationService {

  @NonNull
  private PrivateKeyService privateKeyService;

  @NonNull
  private CertificateService certificateService;

  @NonNull
  private BankConfigurationRepository bankConfigurationRepository;

  @NonNull
  private RepresentativeAccountRepository representativeAccountRepository;

  /**
   * Fetch Default Bank Configuration
   *
   * @return Bank Configuration
   */
  public BankConfigurationEntity getDefaultBankConfigurationForCurrency(String currency) {
    List<BankConfigurationEntity> matching = bankConfigurationRepository
        .fetchBankConfigurationsForCurrency(currency).stream()
        .filter(BankConfigurationEntity::isActive)
        .map(this::enrichBankConfiguration)
        .collect(Collectors.toList());
    if (matching.isEmpty()) {
      log.error("Missing Setup for currency {}", currency);
      throw ServiceException.internalServerError("Missing Setup");
    } else if (matching.size() > 1) {
      log.error("Too many bank configurations ({}) found for currency {}", matching.size(),
          currency);
      throw ServiceException.internalServerError("Wrong Setup");
    } else {
      return matching.get(0);
    }
  }


  /**
   * Fetch Bank Configuration for given Bank Id
   *
   * @return Bank Configuration
   */
  public BankConfigurationEntity getBankConfigurationById(@NonNull String bankId) {
    return getBankConfigurationByIdOrThrow(bankId,
        () -> ServiceException.internalServerError("Missing Setup"));
  }


  /**
   * Fetch Bank Configuration for given Bank Id
   *
   * @return Bank Configuration
   */
  public BankConfigurationEntity getBankConfigurationByIdOrThrow(@NonNull String bankId,
      Supplier<ServiceException> exceptionSupplier) {
    return bankConfigurationRepository.getBankConfiguration(bankId)
        .map(this::enrichBankConfiguration)
        .orElseThrow(() -> {
          log.error("Missing Setup: Couldn't find Configuration for " + bankId);
          return exceptionSupplier.get();
        });
  }


  public BankConfigurationEntity saveBankConfiguration(
      @NonNull BankConfigurationEntity configuration) {
    if (StringUtils.isValid(configuration.getRepresentativeAccountId())) {
      representativeAccountRepository
          .getRepresentativeAccount(configuration.getRepresentativeAccountId())
          .ifPresent(configuration::setRepresentativeAccount);
    }
    bankConfigurationRepository.saveBankConfiguration(configuration);
    return configuration;
  }

  private BankConfigurationEntity enrichBankConfiguration(
      @NonNull BankConfigurationEntity bankConfiguration) {
    PrivateKeyEntity privateKey = privateKeyService
        .getPrivateKey(bankConfiguration.getPrivateKeyId())
        .orElseThrow(() -> {
          log.error("Missing Setup: Couldn't find Private Key for " + bankConfiguration);
          return ServiceException.internalServerError("Missing Setup");
        });
    bankConfiguration.setProxyKey(privateKeyToProxyKey(privateKey));
    return bankConfiguration;
  }

  private ProxyKey privateKeyToProxyKey(PrivateKeyEntity privateKeyEntity) {
    return ProxyKey.builder()
        .id(ProxyId.of(privateKeyEntity.getId(), privateKeyEntity.getCertificateSha256Thumbprint()))
        .name(privateKeyEntity.getName())
        .privateKey(privateKeyEntity.getPrivateKey())
        .privateKeyEncoded(privateKeyEntity.getPrivateKeyEncoded())
        .build();
  }


}
