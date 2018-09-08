package in.yagnyam.proxy.server.banking.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.server.ServiceException;
import in.yagnyam.proxy.server.banking.db.BankConfigurationRepository;
import in.yagnyam.proxy.server.banking.db.RepresentativeAccountRepository;
import in.yagnyam.proxy.server.banking.model.BankConfigurationEntity;
import in.yagnyam.proxy.server.model.PrivateKeyEntity;
import in.yagnyam.proxy.server.services.PrivateKeyService;
import in.yagnyam.proxy.services.CertificateService;
import in.yagnyam.proxy.utils.StringUtils;
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
  private String defaultBankId;

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
   * @return Bank Configuration
   */
  public BankConfigurationEntity getDefaultBankConfiguration() {
    return getBankConfiguration(defaultBankId);
  }

  /**
   * Fetch Default Bank Configuration
   * @return Bank Configuration
   */
  public BankConfigurationEntity getBankConfiguration(@NonNull String bankId) {
    return bankConfigurationRepository.getBankConfiguration(bankId)
        .map(this::enrichBankConfiguration)
        .orElseThrow(() -> {
          log.error("Missing Setup: Couldn't find Configuration for " + bankId);
          return ServiceException.internalServerError("Missing Setup");
        });
  }


  public BankConfigurationEntity saveBankConfiguration(
      @NonNull BankConfigurationEntity configuration) {
    if (StringUtils.isValid(configuration.getRepresentativeAccountId())) {
      representativeAccountRepository
          .getBankConfiguration(configuration.getRepresentativeAccountId())
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
    bankConfiguration.setProxy(fromPrivateKey(privateKey));
    return bankConfiguration;
  }

  private Proxy fromPrivateKey(PrivateKeyEntity privateKeyEntity) {
    Certificate certificate = certificateService
        .getCertificateBySerialNumber(privateKeyEntity.getCertificateSerialNumber())
        .orElseThrow(() -> {
          log.error("Missing Setup: No certificate found for " + privateKeyEntity);
          return ServiceException.internalServerError("Missing Setup");
        });

    return Proxy.builder()
        .privateKey(privateKeyEntity.getPrivateKey())
        .id(ProxyId.of(privateKeyEntity.getId(), certificate.getSha256Thumbprint()))
        .certificateSerialNumber(privateKeyEntity.getCertificateSerialNumber())
        .name(privateKeyEntity.getName())
        .certificate(certificate)
        .build();
  }

}
