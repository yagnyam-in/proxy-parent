package in.yagnyam.proxy.services;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Cryptography Service
 */
public interface CryptographyService {

    /**
     * Key Generation Algorithm
     *
     * @return Default Key Generation Algorithm
     */
    default String getKeyGenerationAlgorithm() {
        return "RSA";
    }

    /**
     * Default Signature algorithm
     *
     * @return Default signature algorithm
     */
    default String getDefaultSignatureAlgorithm() {
        return "SHA256WithRSAEncryption";
    }

    /**
     * Default Algorithm for Encrypting data
     *
     * @return Default Encryption Algorithm
     */
    default String getDefaultEncryptionAlgorithm() {
        return "RSA/NONE/OAEPwithSHA-256andMGF1Padding";
    }

    /**
     * Default Key size
     *
     * @return Key Size
     */
    default int getKeySize() {
        return 2048;
    }


    /**
     * Create Cryptographic Key Pair
     *
     * @return New Key Pair
     * @throws GeneralSecurityException if any error while generating Key Pair
     */
    KeyPair generateKeyPair() throws GeneralSecurityException;


    /**
     * Sign the input text using the private key with provided algorithm and return
     * the signature
     *
     * @param signatureAlgorithm Signing Algorithm
     * @param privateKey         Private Key to Sign
     * @param input              Input Text to sign
     * @return Signature
     * @throws GeneralSecurityException If any Exception while signing
     */
    String getSignature(String signatureAlgorithm, PrivateKey privateKey, String input)
            throws GeneralSecurityException;

    /**
     * Verify the signature for given Input
     *
     * @param signatureAlgorithm Signing Algorithm
     * @param certificate        Public certificate to verify the signature
     * @param input              Input Text that is Signed
     * @param signature          Signature
     * @return true if valid signature, false otherwise
     * @throws GeneralSecurityException if any exception while verifying the
     *                                  signature
     */
    boolean verifySignature(String signatureAlgorithm, Certificate certificate, String input,
                            String signature) throws GeneralSecurityException;

    /**
     * Encrypt with Public Certificate
     *
     * @param encryptionAlgorithm Algorithm to encrypt the input data
     * @param input       Input text to encrypt
     * @param certificate Public certificate to get the Key
     * @return Cipher Text for given input
     * @throws GeneralSecurityException if any exception while encrypting
     */
    String encrypt(String encryptionAlgorithm, Certificate certificate, String input) throws GeneralSecurityException;

    /**
     * Decrypt with Private Key
     *
     * @param encryptionAlgorithm Algorithm used to encrypt the original data
     * @param cipherText Cipher Text to decrypt
     * @param privateKey Private Key
     * @return Original Text
     * @throws GeneralSecurityException if any exception while decrypting
     */
    String decrypt(String encryptionAlgorithm, PrivateKey privateKey, String cipherText) throws GeneralSecurityException;
}
