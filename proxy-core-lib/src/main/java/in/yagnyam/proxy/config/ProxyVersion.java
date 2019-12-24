package in.yagnyam.proxy.config;

import java.util.List;
import java.util.Set;

/**
 * Proxy Configuration
 */
public interface ProxyVersion {

  /**
   * Version
   *
   * @return Proxy Version
   */
  String getVersion();

  /**
   * Valid set of algorithms to digitally sign messages
   *
   * @return List of Algorithm Sets
   */
  List<Set<String>> getValidSignatureAlgorithmSets();

  /**
   * Preferred algorithms to digitally sign message
   *
   * @return Set of algorithms to sign messages
   */
  Set<String> getPreferredSignatureAlgorithmSet();

  /**
   * Valid encryption algorithms to encrypt/decrypt messages
   *
   * @return List of valid encryption algorithms
   */
  List<String> getValidAsymmetricEncryptionAlgorithms();

  /**
   * Returns preferred Encryption Algorithm to encrypt/decrypt messages
   *
   * @return Preferred Encryption Algorithm
   */
  String getPreferredAsymmetricEncryptionAlgorithm();

  /**
   * Valid encryption algorithms to encrypt/decrypt messages
   *
   * @return List of valid encryption algorithms
   */
  List<String> getValidSymmetricEncryptionAlgorithms();

  /**
   * Returns preferred Encryption Algorithm to encrypt/decrypt messages
   *
   * @return Preferred Encryption Algorithm
   */
  String getPreferredSymmetricEncryptionAlgorithm();


  /**
   * Signature algorithm that should be used to create Certificate Request which is part of Proxy
   * Creation Request
   *
   * @return Certificate Signature Algorithm
   */
  String getCertificateSignatureAlgorithm();

  /**
   * Key Generation Algorithm
   *
   * @return Default Key Generation Algorithm
   */
  String getKeyGenerationAlgorithm();

  /**
   * Default Key size
   *
   * @return Key Size
   */
  int getKeySize();

  /**
   * Return latest Proxy Version instance
   *
   * @return Latest Proxy Version Instance
   */
  static ProxyVersion latestVersion() {
    return ProxyVersionV0.instance();
  }

  /**
   * Preferred Hash Algorithm
   *
   * @return Hash Algorithm
   */
  String getPreferredHashAlgorithm();

}
