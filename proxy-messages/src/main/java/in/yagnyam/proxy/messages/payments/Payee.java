package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.Hash;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payee {

  @NonNull
  private String paymentEncashmentId;

  @NonNull
  private PayeeTypeEnum payeeType;

  private ProxyId proxyId;

  private Hash emailHash;

  private Hash phoneHash;

  private Hash secretHash;

  @JsonIgnore
  boolean isValid() {
    if (payeeType == null || StringUtils.isEmpty(paymentEncashmentId)) {
      return false;
    }
    switch (payeeType) {
      case ProxyId:
        return proxyId != null && proxyId.isValid();
      case Email:
        return emailHash != null && emailHash.isValid() && secretHash != null && secretHash.isValid();
      case Phone:
        return phoneHash != null && phoneHash.isValid() && secretHash != null && secretHash.isValid();
      case AnyoneWithSecret:
        return secretHash != null && secretHash.isValid();
      default:
        return false;
    }
  }

}
