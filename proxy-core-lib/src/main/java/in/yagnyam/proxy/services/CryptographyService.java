package in.yagnyam.proxy.services;

import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Cryptography Service
 */
@Slf4j
public abstract class CryptographyService {

    /**
     * Key Generation Algorithm
     * @return
     */
    public String getKeyGenerationAlgorithm() {
        return "RSA";
    }

    /**
     * Default Key size
     * @return
     */
    public int getKeySize() {
        return 2048;
    }

    /**
     * Create Cryptographic Key Pair
     * @return New Key Pair
     * @throws GeneralSecurityException if any error while generating Key Pair
     */
    public abstract KeyPair generateKeyPair() throws GeneralSecurityException;

    /**
     * Create Certificate Request for given
     * @param keyPair Cryptographic Key Pair used for creating CSR
     * @param subject Subject for which the CSR need to be created
     * @return CSR in PEM format
     * @throws GeneralSecurityException if any error while creating CSR
     */
    public abstract String createCertificateRequest(KeyPair keyPair, String subject) throws GeneralSecurityException;

    /**
     * Sign the input text using the private key with provided algorithm and return the signature
     *
     * @param input Input Text to sign
     * @param algorithm Signing Algorithm
     * @param privateKey Private Key to Sign
     * @return Signature
     * @throws GeneralSecurityException If any Exception while signing
     */
    public abstract String getSignature(String input, String algorithm, PrivateKey privateKey) throws GeneralSecurityException;

    /**
     * Verify the signature for given Input
     * @param input Input Text that is Signed
     * @param algorithm Signing Algorithm
     * @param certificate Public certificate to verify the signature
     * @param signature Signature
     * @return true if valid signature, false otherwise
     * @throws GeneralSecurityException if any exception while verifying the signature
     */
    public abstract boolean verifySignature(String input, String algorithm, Certificate certificate, String signature) throws GeneralSecurityException;

}
