package in.yagnyam.proxy.server.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.services.CertificateService;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.UnresolvableKeyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VerificationKeyResolverAdapterTest {


  @Test(expected = IllegalArgumentException.class)
  public void testResolveKey_NoKeyId() throws UnresolvableKeyException {
    VerificationKeyResolverAdapter keyResolver = VerificationKeyResolverAdapter.builder()
        .certificateService(mock(CertificateService.class))
        .build();
    JsonWebSignature jws = mock(JsonWebSignature.class);
    when(jws.getKeyIdHeaderValue()).thenReturn(null);
    keyResolver.resolveKey(jws, Collections.emptyList());
  }

  @Test(expected = UnresolvableKeyException.class)
  public void testResolveKey_NoCertificate() throws UnresolvableKeyException {
    CertificateService certificateService = mock(CertificateService.class);
    when(certificateService.getCertificate("KID", null)).thenReturn(Optional.empty());

    JsonWebSignature jws = mock(JsonWebSignature.class);
    when(jws.getKeyIdHeaderValue()).thenReturn("KID");

    VerificationKeyResolverAdapter keyResolver = VerificationKeyResolverAdapter.builder()
        .certificateService(certificateService)
        .build();
    keyResolver.resolveKey(jws, Collections.emptyList());
  }


  @Test
  public void testResolveKey() throws UnresolvableKeyException {
    PublicKey publicKey = mock(PublicKey.class);
    X509Certificate x509Certificate = mock(X509Certificate.class);
    when(x509Certificate.getPublicKey()).thenReturn(publicKey);

    Certificate certificate = mock(Certificate.class);
    when(certificate.getCertificate()).thenReturn(x509Certificate);

    CertificateService certificateService = mock(CertificateService.class);
    when(certificateService.getCertificate("KID", null))
        .thenReturn(Optional.of(certificate));

    JsonWebSignature jws = mock(JsonWebSignature.class);
    when(jws.getKeyIdHeaderValue()).thenReturn("KID");

    VerificationKeyResolverAdapter keyResolver = VerificationKeyResolverAdapter.builder()
        .certificateService(certificateService)
        .build();
    assert (publicKey == keyResolver.resolveKey(jws, Collections.emptyList()));
  }


  @Test(expected = NullPointerException.class)
  public void testBuilder_WithoutCertificateService() {
    VerificationKeyResolverAdapter.builder().build();
  }

  @Test
  public void testBuilder() {
    VerificationKeyResolverAdapter.builder()
        .certificateService(mock(CertificateService.class))
        .build();
  }

}