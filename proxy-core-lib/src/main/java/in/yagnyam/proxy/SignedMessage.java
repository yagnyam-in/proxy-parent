package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
import java.util.Set;

import lombok.*;

// TODO: Make following fields part of Payload (Don't put them in SignableMessage)
//  1. Signature Timestamp (Should be part of Payload)
//    Advantage: Better Audit/Proof
//  2. Unique Id for Every Message (Should be part of Payload)
//    Advantage: So the Verification can skip already verified messages using Unique Id and Signature Hash.
@Getter
@Builder
@EqualsAndHashCode(exclude = {"message"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(of = {"type", "payload", "signatures"})
public class SignedMessage<T extends SignableMessage> implements ProxyBaseObject {

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

  @JsonIgnore
  private boolean verified = false;

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
    this.verified = false;
    return this;
  }

  @JsonIgnore
  public SignedMessage<T> setVerified(boolean value) {
    this.verified = value;
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
