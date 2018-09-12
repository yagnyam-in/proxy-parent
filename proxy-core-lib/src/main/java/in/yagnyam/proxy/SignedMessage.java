package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString(of = {"type", "payload", "signatures"})
public class SignedMessage<T extends SignableMessage> {

  @JsonIgnore
  private T message;

  @NonNull
  private String type;

  @NonNull
  private String payload;

  @NonNull
  @Singular
  private List<SignedMessageSignature> signatures;

  public ProxyId signer() {
    return message.signer();
  }

  @JsonIgnore
  @SuppressWarnings("unchecked")
  public SignedMessage<T> setMessage(SignableMessage message) {
    this.message = (T) message;
    return this;
  }

  @JsonIgnore
  public boolean isValid() {
    return ((message != null && message.isValid()) || StringUtils.isValid(payload))
        && StringUtils.isValid(type)
        && signatures != null
        && signatures.stream().allMatch(SignedMessageSignature::isValid);
  }


}
