package in.yagnyam.proxy.bootstrap;

import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.ProxyRequest;
import in.yagnyam.proxy.config.ProxyVersion;
import in.yagnyam.proxy.services.CertificateRequestService;
import in.yagnyam.proxy.services.CryptographyService;
import in.yagnyam.proxy.services.PemService;
import lombok.Builder;
import lombok.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

@Builder
public class BcProxyRequestFactory implements ProxyRequestFactory {

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    private PemService pemService;

    @NonNull
    private CertificateRequestService certificateRequestService;

    @NonNull
    private ProxyVersion proxyVersion;

    @Override
    public ProxyRequest createProxyRequest(ProxyKey proxyKey, String revocationPassPhrase) throws GeneralSecurityException, IOException {
        String certificateRequest = certificateRequestService.createCertificateRequest(
                proxyVersion.getCertificateSignatureAlgorithm(),
                new KeyPair(proxyKey.getPublicKey(), proxyKey.getPrivateKey()),
                certificateRequestService.subjectForProxyId(proxyKey.getId().getId())
        );
        return ProxyRequest.builder()
                .id(proxyKey.getId().getId())
                .revocationPassPhraseHash(cryptographyService.getHash(proxyVersion.getPreferredHashAlgorithm(), revocationPassPhrase))
                .requestEncoded(certificateRequest)
                .build();
    }
}
