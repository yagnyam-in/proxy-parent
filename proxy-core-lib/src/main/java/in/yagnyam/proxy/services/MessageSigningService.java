package in.yagnyam.proxy.services;

import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.SignedMessageSignature;
import in.yagnyam.proxy.config.ProxyVersion;
import java.io.IOException;
import java.security.GeneralSecurityException;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

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
  @Default
  private ProxyVersion proxyVersion = ProxyVersion.latestVersion();

  /**
   * Sign the message and produce signed message using the given Proxy
   *
   * @param message Message to Sign
   * @param signer Proxy that is signing the message
   * @param <T> Message type being signed
   * @return Signed Message
   * @throws IOException Any Signing related issues while signing
   */
  public <T extends SignableMessage> SignedMessage<T> sign(T message, ProxyKey signer)
      throws IOException, GeneralSecurityException {
    if (proxyVersion.getPreferredSignatureAlgorithmSet().isEmpty()) {
      throw new IllegalStateException("At least one signature algorithm is required");
    }
    if (!message.isValid()) {
      throw new IllegalStateException("Invalid message: " + message);
    }
    if (!message.cabBeSignedBy(signer.getId())) {
      throw new IllegalStateException("Message: " + message + " can only be signed by "
          + message.validSigners() + ", but trying to sign by " + signer.getId());
    }
    String payload = serializer.serializeSignableMessage(message);
    SignedMessage.SignedMessageBuilder<T> builder = SignedMessage.<T>builder()
        .signedBy(signer.getId())
        .message(message)
        .type(message.getMessageType())
        .payload(payload);
    for (String signatureAlgorithm : proxyVersion.getPreferredSignatureAlgorithmSet()) {
      String signature = cryptographyService
          .getSignature(signatureAlgorithm, signer.getPrivateKey(), payload);
      builder.signature(SignedMessageSignature.of(signatureAlgorithm, signature));
    }
    return builder.build();
  }

}

