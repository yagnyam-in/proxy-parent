package in.yagnyam.proxy.bootstrap;

import in.yagnyam.proxy.ProxyKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface ProxyKeyFactory {

    ProxyKey createProxyKey(
            String id,
            String keyGenerationAlgorithm,
            int keySize
    ) throws GeneralSecurityException, IOException;

}
