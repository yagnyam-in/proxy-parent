package in.yagnyam.proxy.messages.banking;

import in.yagnyam.proxy.SignedMessage;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

/**
 * Wrapper for Holding Proxy Accounts
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class SignedProxyAccounts {

  @Singular
  private List<SignedMessage<ProxyAccount>> accounts;
}
