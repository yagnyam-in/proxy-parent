package in.yagnyam.proxy.services;

import in.yagnyam.proxy.utils.StringUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;


/**
 * Service for creating Certificate Requests
 */
public interface CertificateRequestService {

  /**
   * Create Certificate Request for given Subject
   *
   * @param signatureAlgorithm Signature Algorithm to sign the CSR
   * @param keyPair Cryptographic Key Pair used for creating CSR
   * @param subject Subject for which the CSR need to be created
   * @return CSR in PEM format
   * @throws GeneralSecurityException if any error while creating CSR
   */
  String createCertificateRequest(String signatureAlgorithm, KeyPair keyPair, X500Principal subject)
      throws GeneralSecurityException;

  /**
   * Create Certificate Request for given
   *
   * @param keyPair Cryptographic Key Pair used for creating CSR
   * @param subject Subject for which the CSR need to be created
   * @return CSR in PEM format
   * @throws GeneralSecurityException if any error while creating CSR
   */
  default String createCertificateRequest(KeyPair keyPair, X500Principal subject)
      throws GeneralSecurityException {
    return createCertificateRequest("SHA256WithRSAEncryption", keyPair, subject);
  }


  /**
   * Returns the Subject of the public certificate for given proxy Id
   *
   * @param proxyId Proxy Id
   * @return X509 Certificate subject for given proxy id
   * @throws IOException If any errors while preparing subject
   */
  default X500Principal subjectForProxyId(String proxyId) throws IOException {
    if (StringUtils.isEmpty(proxyId)) {
      throw new IllegalArgumentException("Invalid proxy id [" + proxyId + "]");
    }
    return new X500Principal(
        new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, proxyId).build().getEncoded());
  }


}
