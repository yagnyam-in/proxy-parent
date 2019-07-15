package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
import java.util.Set;

import lombok.*;

// TODO: Add Signature Timestamp to Payload (Or make it a mandatory field in SignableMessage)
@Getter
@Builder
@EqualsAndHashCode(exclude = {"message"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(of = {"type", "payload", "signatures"})
public class SignedMessage<T extends SignableMessage> {

  @JsonIgnore
  private T message;

  @NonNull
  private String type;

  @NonNull
  private String payload;

  @NonNull
  private ProxyId signedBy;

  @NonNull
  @Singular
  private List<SignedMessageSignature> signatures;


  /**
   * Signers that can sign the underlying message.
   * @return Set of Proxies that can sign this message
   */
  public Set<ProxyId> validSigners() {
    return message.validSigners();
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
        && (signedBy != null && signedBy.isValid())
        && StringUtils.isValid(type)
        && signatures != null
        && signatures.stream().allMatch(SignedMessageSignature::isValid);
  }

  /**
   * Test if the Signer can sign this message
   * @param signerId Signer Proxy Id
   * @return true if signer can sign this message
   */
  public boolean canBeSignedBy(ProxyId signerId) {
    return message.cabBeSignedBy(signerId);
  }

}
