package in.yagnyam.proxy.services;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import javax.security.auth.x500.X500Principal;

/**
 * Cryptography Service
 */
public abstract class CryptographyService {

    protected static final String RSA_CIPHER_TRANSFORMATION = "RSA/EBC/OAEPWithSHA1AndMGF1Padding";

    /**
     * Key Generation Algorithm
     *
     * @return Default Key Generation Algorithm
     */
    public String getKeyGenerationAlgorithm() {
        return "RSA";
    }

    /**
     * Default Key size
     *
     * @return Key Size
     */
    public int getKeySize() {
        return 2048;
    }


    /**
     * Create Cryptographic Key Pair
     *
     * @return New Key Pair
     * @throws GeneralSecurityException if any error while generating Key Pair
     */
    public abstract KeyPair generateKeyPair() throws GeneralSecurityException;

    /**
     * Create Certificate Request for given
     *
     * @param keyPair            Cryptographic Key Pair used for creating CSR
     * @param signatureAlgorithm Signature Algorithm to sign the CSR
     * @param subject            Subject for which the CSR need to be created
     * @return CSR in PEM format
     * @throws GeneralSecurityException if any error while creating CSR
     */
    public abstract String createCertificateRequest(KeyPair keyPair, String signatureAlgorithm, X500Principal subject)
            throws GeneralSecurityException;

    /**
     * Sign the input text using the private key with provided algorithm and return
     * the signature
     *
     * @param input              Input Text to sign
     * @param signatureAlgorithm Signing Algorithm
     * @param privateKey         Private Key to Sign
     * @return Signature
     * @throws GeneralSecurityException If any Exception while signing
     */
    public abstract String getSignature(String input, String signatureAlgorithm, PrivateKey privateKey)
            throws GeneralSecurityException;

    /**
     * Verify the signature for given Input
     *
     * @param input              Input Text that is Signed
     * @param signatureAlgorithm Signing Algorithm
     * @param certificate        Public certificate to verify the signature
     * @param signature          Signature
     * @return true if valid signature, false otherwise
     * @throws GeneralSecurityException if any exception while verifying the
     *                                  signature
     */
    public abstract boolean verifySignature(String input, String signatureAlgorithm, Certificate certificate,
            String signature) throws GeneralSecurityException;

    /**
     * Encrypt with Public Certificate
     * 
     * @param input       Input text to encrypt
     * @param certificate Public certificate to get the Key
     * @return Cipher Text for given input
     * @throws GeneralSecurityException if any exception while encrypting
     */
    public abstract String ecnrypt(String input, Certificate certificate) throws GeneralSecurityException;

    /**
     * Decrypt with Private Key
     * 
     * @param cipherText Cipher Text to decrypt
     * @param privateKey Private Key
     * @return Original Text
     * @throws GeneralSecurityException if any exception while decrypting
     */
    public abstract String decrypt(String cipherText, PrivateKey privateKey) throws GeneralSecurityException;
}
