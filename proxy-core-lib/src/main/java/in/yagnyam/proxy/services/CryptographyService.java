package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Hash;
import in.yagnyam.proxy.Hmac;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Cryptography Service
 */
public interface CryptographyService {

  /**
   * Get the Cryptographic Hash of given message
   *
   * @param message Original Message to compute hash
   * @param hashAlgorithm Hashing Algorithm
   * @return Hash
   */
  Hash getHash(String hashAlgorithm, String message) throws GeneralSecurityException;

  /**
   * Verify if the given message produces same Hash
   *
   * @param message Original Message to compute hash
   * @param hash Hash
   * @return Hash
   */
  boolean verifyHash(String message, Hash hash) throws GeneralSecurityException;

  /**
   * Get the HMAC of given message and key
   *
   * @param key Secret Key
   * @param message Original Message to compute hash
   * @param hmacAlgorithm HMAC Algorithm
   * @return Hash
   */
  Hmac getHmac(String hmacAlgorithm, String key, String message) throws GeneralSecurityException;

  /**
   * Validates the Key by verifying the HMAC
   * @param key Secret Key
   * @param hmac Hmac
   * @return true if
   */
  boolean verifyHmac(String key, String message, Hmac hmac) throws GeneralSecurityException;

  /**
   * Create Cryptographic Key Pair
   *
   * @param keyGenerationAlgorithm Key Generation Algorithm
   * @param keySize Key Size
   * @return New Key Pair
   * @throws GeneralSecurityException if any error while generating Key Pair
   */
  KeyPair generateKeyPair(String keyGenerationAlgorithm, int keySize)
      throws GeneralSecurityException;


  /**
   * Sign the input text using the private key with provided algorithm and return the signature
   *
   * @param signatureAlgorithm Signing Algorithm
   * @param privateKey Private Key to Sign
   * @param input Input Text to sign
   * @return Signature
   * @throws GeneralSecurityException If any Exception while signing
   */
  String getSignature(String signatureAlgorithm, PrivateKey privateKey, String input)
      throws GeneralSecurityException;

  /**
   * Verify the signature for given Input
   *
   * @param signatureAlgorithm Signing Algorithm
   * @param certificate Public certificate to verify the signature
   * @param input Input Text that is Signed
   * @param signature Signature
   * @return true if valid signature, false otherwise
   * @throws GeneralSecurityException if any exception while verifying the signature
   */
  boolean verifySignature(String signatureAlgorithm, Certificate certificate, String input,
      String signature) throws GeneralSecurityException;

  /**
   * Encrypt with Public Certificate
   *
   * @param encryptionAlgorithm Algorithm to encrypt the input data
   * @param input Input text to encrypt
   * @param certificate Public certificate to get the Key
   * @return Cipher Text for given input
   * @throws GeneralSecurityException if any exception while encrypting
   */
  String encrypt(String encryptionAlgorithm, Certificate certificate, String input)
      throws GeneralSecurityException;

  /**
   * Decrypt with Private Key
   *
   * @param encryptionAlgorithm Algorithm used to encrypt the original data
   * @param cipherText Cipher Text to decrypt
   * @param privateKey Private Key
   * @return Original Text
   * @throws GeneralSecurityException if any exception while decrypting
   */
  String decrypt(String encryptionAlgorithm, PrivateKey privateKey, String cipherText)
      throws GeneralSecurityException;
}
