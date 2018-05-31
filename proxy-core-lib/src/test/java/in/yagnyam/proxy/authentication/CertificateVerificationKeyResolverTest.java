package in.yagnyam.proxy.authentication;


import lombok.extern.slf4j.Slf4j;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.UnresolvableKeyException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

@Slf4j
public class CertificateVerificationKeyResolverTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testResolveKey_NoKeyId() throws IOException, UnresolvableKeyException {
        CertificateResolver certificateResolver = mock(CertificateResolver.class);
        CertificateVerificationKeyResolver keyResolver = CertificateVerificationKeyResolver.builder()
                .certificateResolver(certificateResolver)
                .build();
        JsonWebSignature jws = mock(JsonWebSignature.class);
        when (jws.getKeyIdHeaderValue()).thenReturn(null);

        exception.expect(IllegalArgumentException.class);
        keyResolver.resolveKey(jws, Collections.emptyList());
    }

    @Test
    public void testResolveKey_NoCertificate() throws IOException, UnresolvableKeyException {
        CertificateResolver certificateResolver = mock(CertificateResolver.class);
        when (certificateResolver.getCertificate("123")).thenReturn(Optional.empty());
        CertificateVerificationKeyResolver keyResolver = CertificateVerificationKeyResolver.builder()
                .certificateResolver(certificateResolver)
                .build();
        JsonWebSignature jws = mock(JsonWebSignature.class);
        when (jws.getKeyIdHeaderValue()).thenReturn("123");

        exception.expect(UnresolvableKeyException.class);
        keyResolver.resolveKey(jws, Collections.emptyList());
    }


    @Test
    public void testResolveKey() throws IOException, UnresolvableKeyException {
        X509Certificate certificate = mock(X509Certificate.class);

        CertificateResolver certificateResolver = mock(CertificateResolver.class);
        when (certificateResolver.getCertificate("123")).thenReturn(Optional.of(certificate));

        CertificateVerificationKeyResolver keyResolver = CertificateVerificationKeyResolver.builder()
                .certificateResolver(certificateResolver)
                .build();

        JsonWebSignature jws = mock(JsonWebSignature.class);
        when (jws.getKeyIdHeaderValue()).thenReturn("123");

        keyResolver.resolveKey(jws, Collections.emptyList());
        verify(certificate, times(1)).getPublicKey();
    }
}
