package in.yagnyam.proxy.messages.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableAlertMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PendingAlertsResponse implements SignableMessage, AddressableMessage {

  @NonNull
  private SignedMessage<PendingAlertsRequest> request;

  @NonNull
  private List<SignedMessage<SignableAlertMessage>> alerts;

  @NonNull
  private Date tillTime;

  @Override
  public ProxyId signer() {
    return request.getMessage().getAlertProviderProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return ProxyUtils.isValid(request)
        && alerts != null
        && alerts.stream().allMatch(SignedMessage::isValid)
        && DateUtils.isValid(tillTime);
  }

  @Override
  public ProxyId address() {
    return request.getSignedBy();
  }
}
