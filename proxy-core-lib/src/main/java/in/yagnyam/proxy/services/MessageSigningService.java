package in.yagnyam.proxy.services;

import in.yagnyam.proxy.*;
import in.yagnyam.proxy.config.ProxyVersion;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

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
  public <T extends SignableMessage> SignedMessage<T> singleSign(T message, ProxyKey signer)
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


  /**
   * Sign the message and produce signed message using the given Proxy
   *
   * @param message Message to Sign
   * @param signer Proxy that is signing the message
   * @param <T> Message type being signed
   * @return Signed Message
   * @throws IOException Any Signing related issues while signing
   */
  public <T extends MultiSignableMessage> MultiSignedMessage<T> multiSign(T message, ProxyKey signer)
          throws IOException, GeneralSecurityException {
    String payload = serializer.serializeMultiSignableMessage(message);
    MultiSignedMessageSignature signature = _multiSign(message, payload, signer);
    return MultiSignedMessage.<T>builder()
            .message(message)
            .type(message.getMessageType())
            .payload(payload)
            .signature(signature)
            .build();
  }

  /**
   * Sign the message and produce signed message using the given Proxy
   *
   * @param signedMessage Signed Message to add Signature
   * @param signer Proxy that is signing the message
   * @param <T> Message type being signed
   * @return Signed Message
   */
  public <T extends MultiSignableMessage> MultiSignedMessage<T> multiSign(MultiSignedMessage<T> signedMessage, ProxyKey signer)
          throws GeneralSecurityException {
    if (signedMessage.getMessage() == null) {
      throw new IllegalStateException("Message should be deserialized before signing");
    }
    return signedMessage.toBuilder().signature(_multiSign(signedMessage.getMessage(), signedMessage.getPayload(), signer)).build();
  }


  private  <T extends MultiSignableMessage> MultiSignedMessageSignature _multiSign(T message, String payload, ProxyKey signer)
          throws GeneralSecurityException {
    if (proxyVersion.getPreferredSignatureAlgorithmSet().isEmpty()) {
      throw new IllegalStateException("At least one signature algorithm is required");
    }
    if (!message.isValid()) {
      throw new IllegalStateException("Invalid message: " + message);
    }
    if (!message.canBeSignedBy(signer.getId())) {
      throw new IllegalStateException("Message: " + message + " can not be sign by " + signer.getId());
    }
    if (!StringUtils.isValid(payload)) {
      throw new IllegalStateException("Invalid Payload: " + payload);
    }
    List<SignedMessageSignature> signatures = new ArrayList<>();
    for (String signatureAlgorithm : proxyVersion.getPreferredSignatureAlgorithmSet()) {
      String signature = cryptographyService
              .getSignature(signatureAlgorithm, signer.getPrivateKey(), payload);
      signatures.add(SignedMessageSignature.of(signatureAlgorithm, signature));
    }
    return MultiSignedMessageSignature.of(signer.getId(), signatures);
  }


}

