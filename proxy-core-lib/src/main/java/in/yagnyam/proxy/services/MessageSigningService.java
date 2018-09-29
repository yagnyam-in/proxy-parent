package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.SignedMessageSignature;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * Service to sign the message
 */
@Builder
public class MessageSigningService {

  @NonNull
  private MessageSerializerService serializer;

  @NonNull
  private CryptographyService cryptographyService;

  @NonNull
  @Singular
  private List<String> signatureAlgorithms;

  /**
   * Sign the message and produce signed message using the given Proxy
   *
   * @param message Message to Sign
   * @param signer Proxy that is signing the message
   * @param <T> Message type being signed
   * @return Signed Message
   * @throws IOException Any Signing related issues while signing
   */
  public <T extends SignableMessage> SignedMessage<T> sign(T message, Proxy signer)
      throws IOException, GeneralSecurityException {
    if (signatureAlgorithms.isEmpty()) {
      throw new IllegalStateException("At least one signature algorithm is required");
    }
    if (!message.isValid()) {
      throw new IllegalStateException("Invalid message: " + message);
    }
    if (!message.signer().isParentOrEqualsOf(signer.getId())) {
      throw new IllegalStateException("Message: " + message + " can only be signed by "
          + message.signer() + ", but trying to sign by " + signer.getId());
    }
    String payload = serializer.serializeSignableMessage(message);
    SignedMessage.SignedMessageBuilder<T> builder = SignedMessage.<T>builder()
        .signedBy(signer.getId())
        .message(message)
        .type(message.getMessageType())
        .payload(payload);
    for (String signatureAlgorithm : signatureAlgorithms) {
      String signature = cryptographyService
          .getSignature(signatureAlgorithm, signer.getPrivateKey(), payload);
      builder.signature(SignedMessageSignature.of(signatureAlgorithm, signature));
    }
    return builder.build();
  }
}

