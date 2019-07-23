package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountId {

  @NonNull
  private String proxyUniverse;

  @NonNull
  private String bankId;

  @NonNull
  private String accountId;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(proxyUniverse)
        && StringUtils.isValid(bankId)
        && StringUtils
        .isValid(accountId);
  }
}
