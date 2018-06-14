package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * Message Verification service to Verify Signature on Signed Messages
 */
@Builder
@Slf4j
public class MessageVerificationService {

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    private ProxyResolver proxyResolver;

    @NonNull
    @Singular
    private List<String> signatureAlgorithms;

    /**
     * Verify Signature on Singed Messages
     * @param message Signed Message to verify signature
     * @param <T> Underlying Message Type
     * @throws IOException In case of any exceptions while validating Signature
     * @throws IllegalArgumentException If Signatures are missing or wrong
     */
    public <T extends SignableMessage> void verify(@NonNull SignedMessage<T> message) throws IOException {
        if (signatureAlgorithms.isEmpty()) {
            log.error("At least one signature algorithm is required");
            throw new IllegalStateException("At least one signature algorithm is required");
        }
        Proxy proxy = getSignerProxy(message);
        for (String algorithm : signatureAlgorithms) {
            SignedMessage.Signature signature = findSignature(message, algorithm);
            if (!cryptographyService.verifySignature(message.getPayload().getBytes(), algorithm, proxy.getCertificate().getCertificate(), signature.getValue())) {
                log.error("Invalid Signature on " + message);
                throw new IllegalArgumentException("Invalid Signature");
            }
        }
        signatureAlgorithms.forEach(algorithm -> {

        });
    }


    /**
     * Find Signature for given algorithm from signed message
     * @param signedMessage Signed Message
     * @param signatureAlgorithm Signature Algorithm
     * @param <T> Underlying Message Type
     * @return Signature for given algorithm
     * @throws IllegalArgumentException If Signature not found
     */
    <T extends SignableMessage> SignedMessage.Signature findSignature(SignedMessage<T> signedMessage, @NonNull String signatureAlgorithm) throws IllegalArgumentException {
        for (SignedMessage.Signature signature : signedMessage.getSignatures()) {
            if (signature.getAlgorithm().equalsIgnoreCase(signatureAlgorithm)) {
                return signature;
            }
        }
        log.error("No Signature for algorithm " + signatureAlgorithm + " in " + signedMessage);
        throw new IllegalArgumentException("No Signature for algorithm " + signatureAlgorithm);
    }


    /**
     * Get Proxy for signer
     *
     * @param message Signed Message
     * @return Proxy matching the signature
     */
    Proxy getSignerProxy(SignedMessage message) {
        if (message.getMessage() == null) {
            log.error("SignedMessage must be de-serialized before verifying signature");
            throw new IllegalStateException("SignedMessage must be de-serialized before verifying signature");
        }
        List<Proxy> proxies = proxyResolver.resolveProxy(message.signer());
        if (proxies.isEmpty()) {
            log.error("Invalid Signer/Proxy Id. No proxies found.");
            throw new IllegalArgumentException("Invalid Signer/Proxy Id. No proxies found.");
        } else if (proxies.size() != 1) {
            log.error("Incomplete Signer/Proxy Id. Multiple proxies found.");
            throw new IllegalArgumentException("Incomplete Signer/Proxy Id. Multiple proxies found.");
        } else {
            return proxies.get(0);
        }
    }
}
