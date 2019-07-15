package in.yagnyam.proxy.services;

import in.yagnyam.proxy.*;
import in.yagnyam.proxy.config.ProxyVersion;
import lombok.*;
import lombok.Builder.Default;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Message Verification service to Verify Signature on Signed Messages
 */
@Slf4j
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageVerificationService {

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    private ProxyResolver proxyResolver;

    @NonNull
    @Default
    private ProxyVersion proxyVersion = ProxyVersion.latestVersion();

    /**
     * Verify Signature on Singed Messages
     *
     * @param message Signed Message to verify signature
     * @param <T>     Underlying Message Type
     * @throws GeneralSecurityException In case of any exceptions while validating Signature
     * @throws IllegalArgumentException If Signatures are missing or wrong
     */
    public <T extends SignableMessage> void verify(@NonNull SignedMessage<T> message)
            throws GeneralSecurityException {
        if (!message.isValid()) {
            log.info("Message validation failed for {}", message);
            throw new IllegalArgumentException("Message validation failed");
        }
        if (message.getSignatures().isEmpty()) {
            log.info("No signatures found in {}", message);
            throw new IllegalArgumentException("No signatures found");
        }
        Set<String> signatureAlgorithmSet = message.getSignatures().stream()
                .map(SignedMessageSignature::getAlgorithm)
                .collect(Collectors.toSet());
        if (!proxyVersion.getValidSignatureAlgorithmSets()
                .contains(signatureAlgorithmSet)) {
            log.info("Un-recognised signature algorithms {} in {}", signatureAlgorithmSet, message);
            throw new IllegalArgumentException(
                    "Un-recognised signature algorithms " + signatureAlgorithmSet);
        }
        if (!message.canBeSignedBy(message.getSignedBy())) {
            throw new IllegalStateException("Message: " + message + " can only be signed by "
                    + message.validSigners() + ", but signed by " + message.getSignedBy());
        }
        validateSignaturesBySingleProxy(message.getSignedBy(), message.getPayload(), message.getSignatures(), message);
    }


    /**
     * Verify Signatures on Multi Singed Message.
     * <p>
     * Is Valid doesn't mean it is complete. It might still be lacking required signatures.
     *
     * @param message Multi Signed Message to verify signatures
     * @param <T>     Underlying Message Type
     * @throws GeneralSecurityException In case of any exceptions while validating Signature
     * @throws IllegalArgumentException If Signatures are missing or wrong
     */
    public <T extends MultiSignableMessage> void isValid(@NonNull MultiSignedMessage<T> message)
            throws GeneralSecurityException {
        if (!message.isValid()) {
            log.info("Message validation failed for {}", message);
            throw new IllegalArgumentException("Message validation failed");
        }
        validateSignatures(message);
    }


    /**
     * Check if the Multi Signed message has required valid signatures.
     *
     * @param message Multi Signed Message to verify signatures
     * @param <T>     Underlying Message Type
     * @throws GeneralSecurityException In case of any exceptions while validating Signature
     * @throws IllegalArgumentException If Signatures are missing or wrong
     */
    public <T extends MultiSignableMessage> void isComplete(@NonNull MultiSignedMessage<T> message)
            throws GeneralSecurityException {
        if (!message.isComplete()) {
            log.info("Message validation failed for {}", message);
            throw new IllegalArgumentException("Message validation failed");
        }
        validateSignatures(message);
    }


    private <T extends MultiSignableMessage> void validateSignatures(@NonNull MultiSignedMessage<T> message) throws GeneralSecurityException {
        for (MultiSignedMessageSignature multiSignature : message.getSignatures()) {
            validateMultiSignature(message, multiSignature);
        }
    }

    private <T extends MultiSignableMessage> void validateMultiSignature(@NonNull MultiSignedMessage<T> message,
                                                                         @NonNull MultiSignedMessageSignature multiSignature) throws GeneralSecurityException {
        Set<String> signatureAlgorithmSet = multiSignature.getSignatures().stream()
                .map(SignedMessageSignature::getAlgorithm)
                .collect(Collectors.toSet());
        if (!proxyVersion.getValidSignatureAlgorithmSets()
                .contains(signatureAlgorithmSet)) {
            log.info("Un-recognised signature algorithms {} in {}", signatureAlgorithmSet, message);
            throw new IllegalArgumentException(
                    "Un-recognised signature algorithms " + signatureAlgorithmSet);
        }
        validateSignaturesBySingleProxy(multiSignature.getSignedBy(), message.getPayload(), multiSignature.getSignatures(), message);
    }

    private void validateSignaturesBySingleProxy(ProxyId signerProxyId, String payload, List<SignedMessageSignature> signatures, Object signedMessage) throws GeneralSecurityException {
        Proxy proxy = getSignerProxy(signerProxyId);
        for (SignedMessageSignature signature : signatures) {
            if (!cryptographyService.verifySignature(signature.getAlgorithm(),
                    proxy.getCertificate().getCertificate(),
                    payload,
                    signature.getValue())) {
                log.error("Invalid Signature on " + signedMessage);
                throw new IllegalArgumentException("Invalid Signature");
            }
        }
    }


    /**
     * Get Proxy for signer
     *
     * @param signerProxyId Signer Proxy Id
     * @return Proxy matching the signature
     */
    private Proxy getSignerProxy(ProxyId signerProxyId) {
        Optional<Proxy> proxy = proxyResolver.resolveProxy(signerProxyId);
        if (!proxy.isPresent()) {
            log.error("Invalid Signer/Proxy Id. No proxy found.");
            throw new IllegalArgumentException("Invalid Signer/Proxy Id. No proxy found.");
        }
        return proxy.get();
    }


}
