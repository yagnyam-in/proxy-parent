package in.yagnyam.proxy.services;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Cryptography Service using Bouncy Castle
 */
@Slf4j
@Builder
public class BcCryptographyService implements CryptographyService {

  private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
  private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();
  private static final Map<String, String> encryptionAlgorithmMapping = new HashMap<String, String>() {
    private static final long serialVersionUID = 1L;

    {
      put("RSA/NONE/OAEPwithSHA-256andMGF1Padding", "RSA/NONE/OAEPwithSHA-256andMGF1Padding");
    }
  };

  static {
    Security.addProvider(BC_PROVIDER);
  }

  @Override
  public KeyPair generateKeyPair(String keyGenerationAlgorithm, int keySize)
      throws GeneralSecurityException {
    KeyPairGenerator generator = KeyPairGenerator
        .getInstance(keyGenerationAlgorithm, BC_PROVIDER);
    generator.initialize(keySize, new SecureRandom());
    return generator.generateKeyPair();
  }

  @Override
  public String getHash(String hashAlgorithm, String message) throws GeneralSecurityException {
    MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
    byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
    return Base64.toBase64String(hash);
  }


  @Override
  public String getHmac(String hmacAlgorithm, String key, String message) throws GeneralSecurityException {
    Mac mac = Mac.getInstance(hmacAlgorithm);
    SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), hmacAlgorithm);
    mac.init(secretKey);
    byte[] hmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    return Base64.toBase64String(hmac);
  }


  @Override
  public String getSignature(String algorithm, PrivateKey privateKey, String input)
      throws GeneralSecurityException {
    try {
      Signature signature = getSignatureInstance(algorithm);
      signature.initSign(privateKey);
      signature.update(input.getBytes(DEFAULT_CHARSET));
      return Base64.toBase64String(signature.sign());
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      log.error("Error signing for algorithm " + algorithm, e);
      throw new GeneralSecurityException(e);
    }
  }

  @Override
  public boolean verifySignature(String algorithm, Certificate certificate, String input,
      String signature)
      throws GeneralSecurityException {
    try {
      Signature signatureInstance = getSignatureInstance(algorithm);
      signatureInstance.initVerify(certificate);
      signatureInstance.update(input.getBytes(DEFAULT_CHARSET));
      return signatureInstance.verify(Base64.decode(signature));
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      log.error("Error verifying signature of algorithm " + algorithm, e);
      throw new GeneralSecurityException(e);
    }
  }

  @Override
  public String encrypt(String encryptionAlgorithm, Certificate certificate, String input)
      throws GeneralSecurityException {
    Cipher cipher = getCipherInstance(encryptionAlgorithm);
    cipher.init(Cipher.ENCRYPT_MODE, certificate);
    byte[] cipherText = cipher.doFinal(input.getBytes(DEFAULT_CHARSET));
    return Base64.toBase64String(cipherText);
  }

  @Override
  public String decrypt(String encryptionAlgorithm, PrivateKey privateKey, String cipherText)
      throws GeneralSecurityException {
    Cipher cipher = getCipherInstance(encryptionAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    byte[] originalText = cipher.doFinal(Base64.decode(cipherText.getBytes(DEFAULT_CHARSET)));
    return new String(originalText, DEFAULT_CHARSET);
  }

  private Signature getSignatureInstance(String algorithm) throws NoSuchAlgorithmException {
    return Signature.getInstance(algorithm, BC_PROVIDER);
  }

  private Cipher getCipherInstance(String algorithm)
      throws NoSuchAlgorithmException, NoSuchPaddingException {
    String effectiveAlgorithm = encryptionAlgorithmMapping.get(algorithm);
    if (effectiveAlgorithm == null) {
      throw new NoSuchAlgorithmException(algorithm + " is not supported");
    }
    return Cipher.getInstance(effectiveAlgorithm, BC_PROVIDER);
  }

}
