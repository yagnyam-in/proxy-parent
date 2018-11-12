package in.yagnyam.proxy.server.banking.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.AccountBalanceRequest;
import in.yagnyam.proxy.messages.banking.AccountBalanceResponse;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.Currency;
import in.yagnyam.proxy.messages.banking.ProxyAccountCreationRequest;
import in.yagnyam.proxy.messages.banking.ProxyAccountCreationResponse;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.server.ServiceException;
import in.yagnyam.proxy.server.banking.db.ProxyAccountRepository;
import in.yagnyam.proxy.server.banking.model.BankConfigurationEntity;
import in.yagnyam.proxy.server.banking.model.OriginalAccountEntity;
import in.yagnyam.proxy.server.banking.model.ProxyAccountEntity;
import in.yagnyam.proxy.services.MessageSigningService;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.RandomUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class ProxyAccountService {

  @NonNull
  private ProxyAccountRepository proxyAccountRepository;

  @NonNull
  private BankConfigurationService bankConfigurationService;

  @NonNull
  private MessageSigningService messageSigningService;

  /**
   * Get Proxy Account for given Proxy Account Id
   * @param proxyAccountId Proxy Account Id
   * @return Proxy Account for given Id
   * @throws in.yagnyam.proxy.server.BadRequestException if there is no Account for given Id
   */
  public ProxyAccountEntity getProxyAccountEntity(ProxyAccountId proxyAccountId) {
    return proxyAccountRepository.fetchProxyAccount(proxyAccountId)
        .orElseThrow(() -> ServiceException.badRequest("No Such account exists"));
  }

  public SignedMessage<AccountBalanceResponse> fetchProxyAccountBalance(
      AccountBalanceRequest request) {

    ProxyAccountId proxyAccountId = request.proxyAccount.getMessage().getProxyAccountId();

    BankConfigurationEntity bankConfiguration =
        bankConfigurationService.getBankConfiguration(proxyAccountId.getBankId());

    ProxyAccountEntity proxyAccountEntity = getProxyAccountEntity(proxyAccountId);

    AccountBalanceResponse response = AccountBalanceResponse.builder()
        .requestId(request.requestId())
        .proxyAccount(request.proxyAccount)
        .balance(proxyAccountEntity.getOriginalAccountEntity().getBalance())
        .build();

    return sign(response, bankConfiguration.getProxy());
  }

  public SignedMessage<ProxyAccountCreationResponse> createProxyAccount(
      ProxyAccountCreationRequest request) {

    assertValidRequest(request);

    BankConfigurationEntity bankConfiguration =
        bankConfigurationService.getBankConfiguration(request.getCurrency());

    ProxyAccountEntity accountEntity = createAccount(request, bankConfiguration);

    ProxyAccountCreationResponse response = ProxyAccountCreationResponse.builder()
        .requestId(request.requestId())
        .accountNumber(accountEntity.getOriginalAccountEntity().getAccountId())
        .bankName(accountEntity.getOriginalAccountEntity().getBank())
        .balance(accountEntity.getOriginalAccountEntity().getBalance())
        .proxyAccount(sign(accountEntity.asProxyAccount(), bankConfiguration.getProxy()))
        .build();

    return sign(response, bankConfiguration.getProxy());
  }

  private String randomAccountId() {
    String accountId = RandomUtils.randomString();
    while (proxyAccountRepository.fetchProxyAccount(accountId).isPresent()) {
      accountId = RandomUtils.randomString();
    }
    return accountId;
  }

  private ProxyAccountEntity createAccount(ProxyAccountCreationRequest request,
      BankConfigurationEntity bankConfiguration) {

    String accountId = randomAccountId();

    OriginalAccountEntity underlyingAccount = OriginalAccountEntity.builder()
        .accountId(accountId)
        .accountNumber(accountId)
        .bank(bankConfiguration.getBankName())
        .accountHolder(request.getProxyId().getId())
        .currency(request.getCurrency())
        .balance(Amount.of(request.getCurrency(), 0))
        .build();

    ProxyAccountEntity proxyAccountEntity = ProxyAccountEntity.builder()
        .proxyAccountId(accountId)
        .bankId(bankConfiguration.getBankId())
        .proxyId(request.getProxyId())
        .currency(request.getCurrency())
        .creationDate(DateUtils.now())
        .expiryDate(DateUtils.afterYears(1))
        .maximumAmountPerTransaction(Amount.of(request.getCurrency(), 100))
        .originalAccountEntity(underlyingAccount)
        .originalAccountId(accountId)
        .build();

    proxyAccountRepository.saveProxyAccountWithLinkedAccount(proxyAccountEntity, underlyingAccount);

    return proxyAccountEntity;

  }

  private <T extends SignableMessage> SignedMessage<T> sign(T message, Proxy signer) {
    try {
      return messageSigningService.sign(message, signer);
    } catch (IOException | GeneralSecurityException e) {
      log.error("Error while signing Response", e);
      throw ServiceException.internalServerError("Unable to sign");
    }
  }


  static void assertValidRequest(ProxyAccountCreationRequest request) {
    if (!Currency.isValidCurrency(request.getCurrency())) {
      throw ServiceException.badRequest("Invalid currency " + request.getCurrency());
    }
  }

}
