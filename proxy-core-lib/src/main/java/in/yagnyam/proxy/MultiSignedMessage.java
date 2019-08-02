package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(exclude = {"message"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(of = {"type", "payload", "signatures"})
public class MultiSignedMessage<T extends MultiSignableMessage> implements ProxyBaseObject {

  @JsonIgnore
  private T message;

  @NonNull
  private String type;

  @NonNull
  private String payload;

  @NonNull
  @Singular
  private List<MultiSignedMessageSignature> signatures;

  @JsonIgnore
  private boolean verified = false;

  public Set<ProxyId> actualSigners() {
    return signatures.stream().map(MultiSignedMessageSignature::getSignedBy).collect(Collectors.toSet());
  }

  @JsonIgnore
  @SuppressWarnings("unchecked")
  public MultiSignedMessage<T> setMessage(MultiSignableMessage message) {
    this.message = (T) message;
    this.verified = false;
    return this;
  }

  @JsonIgnore
  public MultiSignedMessage<T> setVerified(boolean value) {
    this.verified = value;
    return this;
  }

  @JsonIgnore
  public boolean hasSufficientSignatures() {
    Set<ProxyId> actualSigners = actualSigners();
    return message.hasSufficientSignatures(actualSigners);
  }

  @JsonIgnore
  @Override
  public boolean isValid() {
    if (message == null) {
      throw new IllegalArgumentException("Message should be de-serialized to check validity");
    }
    return (message.isValid() || StringUtils.isValid(payload)) && StringUtils.isValid(type) && signatures != null && signatures.stream().allMatch(MultiSignedMessageSignature::isValid);
  }

}
