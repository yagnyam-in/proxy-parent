package in.yagnyam.proxy.bootstrap;

import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.ProxyRequest;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface ProxyRequestFactory {
    ProxyRequest createProxyRequest(
            ProxyKey proxyKey,
            String revocationPassPhrase
    ) throws GeneralSecurityException, IOException;
}
