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
     * @param signedMessage Signed Message to verify signature
     * @param <T>           Underlying Message Type
     * @throws GeneralSecurityException In case of any exceptions while validating Signature
     * @throws IllegalArgumentException If Signatures are missing or wrong
     */
    public <T extends SignableMessage> void verify(@NonNull SignedMessage<T> signedMessage)
            throws GeneralSecurityException {
        if (signedMessage.isVerified()) {
            log.info("Message {} already verified", signedMessage);
            return;
        }
        if (!signedMessage.isValid()) {
            log.info("Message validation failed for {}", signedMessage);
            throw new IllegalArgumentException("Message validation failed");
        }
        if (signedMessage.getSignatures().isEmpty()) {
            log.info("No signatures found in {}", signedMessage);
            throw new IllegalArgumentException("No signatures found");
        }
        if (signedMessage.getMessage() == null) {
            log.info("Message {} must be de-serialized before verify", signedMessage);
            throw new IllegalStateException("Message must be de-serialized before verify");
        }
        Set<String> signatureAlgorithmSet = signedMessage.getSignatures().stream()
                .map(SignedMessageSignature::getAlgorithm)
                .collect(Collectors.toSet());
        if (!proxyVersion.getValidSignatureAlgorithmSets()
                .contains(signatureAlgorithmSet)) {
            log.info("Un-recognised signature algorithms {} in {}", signatureAlgorithmSet, signedMessage);
            throw new IllegalArgumentException(
                    "Un-recognised signature algorithms " + signatureAlgorithmSet);
        }
        if (!signedMessage.canBeSignedBy(signedMessage.getSignedBy())) {
            throw new IllegalStateException("Message: " + signedMessage + " can only be signed by "
                    + signedMessage.validSigners() + ", but signed by " + signedMessage.getSignedBy());
        }
        validateSignaturesBySingleProxy(signedMessage.getSignedBy(), signedMessage.getPayload(),
                signedMessage.getSignatures(), signedMessage);
        // TODO: Double check this
        signedMessage.setVerified(true);
    }


    /**
     * Verify Signatures on Multi Singed Message.
     * <p>
     * Verified doesn't mean it is complete. It might still be lacking required signatures.
     *
     * @param multiSignedMessage Multi Signed Message to verify signatures
     * @param <T>                Underlying Message Type
     * @throws GeneralSecurityException In case of any exceptions while validating Signature
     * @throws IllegalArgumentException If Signatures are missing or wrong
     */
    public <T extends MultiSignableMessage> void verify(@NonNull MultiSignedMessage<T> multiSignedMessage)
            throws GeneralSecurityException {
        if (multiSignedMessage.isVerified()) {
            log.info("Message {} already verified", multiSignedMessage);
            return;
        }
        if (!multiSignedMessage.isValid()) {
            log.info("Message validation failed for {}", multiSignedMessage);
            throw new IllegalArgumentException("Message validation failed");
        }
        if (multiSignedMessage.getMessage() == null) {
            log.info("Message {} must be de-serialized before verify", multiSignedMessage);
            throw new IllegalStateException("Message must be de-serialized before verify");
        }
        validateMultiSignatures(multiSignedMessage);
        // TODO: Double check this
        multiSignedMessage.setVerified(true);
    }


    private <T extends MultiSignableMessage> void validateMultiSignatures(
            @NonNull MultiSignedMessage<T> multiSignedMessage)
            throws GeneralSecurityException {
        for (MultiSignedMessageSignature multiSignature : multiSignedMessage.getSignatures()) {
            validateMultiSignature(multiSignedMessage, multiSignature);
        }
    }

    private <T extends MultiSignableMessage> void validateMultiSignature(
            @NonNull MultiSignedMessage<T> message,
            @NonNull MultiSignedMessageSignature multiSignature
    ) throws GeneralSecurityException {
        Set<String> signatureAlgorithmSet = multiSignature.getSignatures().stream()
                .map(SignedMessageSignature::getAlgorithm)
                .collect(Collectors.toSet());
        if (!proxyVersion.getValidSignatureAlgorithmSets()
                .contains(signatureAlgorithmSet)) {
            log.info("Un-recognised signature algorithms {} in {}", signatureAlgorithmSet, message);
            throw new IllegalArgumentException(
                    "Un-recognised signature algorithms " + signatureAlgorithmSet);
        }
        validateSignaturesBySingleProxy(multiSignature.getSignedBy(), message.getPayload(), multiSignature.getSignatures(),
                message);
    }

    private void validateSignaturesBySingleProxy(ProxyId signerProxyId, String payload,
                                                 List<SignedMessageSignature> signatures, Object signedMessage) throws GeneralSecurityException {
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


    private Proxy getSignerProxy(ProxyId signerProxyId) {
        Optional<Proxy> proxy = proxyResolver.resolveProxy(signerProxyId);
        if (!proxy.isPresent()) {
            log.error("Invalid Signer Proxy Id. No proxy found.");
            throw new IllegalArgumentException("Invalid Signer Proxy Id. No proxy found.");
        }
        return proxy.get();
    }


}
