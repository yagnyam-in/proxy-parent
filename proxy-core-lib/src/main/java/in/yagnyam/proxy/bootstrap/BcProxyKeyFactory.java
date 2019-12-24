package in.yagnyam.proxy.bootstrap;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.services.CryptographyService;
import in.yagnyam.proxy.services.PemService;
import lombok.Builder;
import lombok.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

@Builder
public class BcProxyKeyFactory implements ProxyKeyFactory {

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    private PemService pemService;

    @Override
    public ProxyKey createProxyKey(String id, String keyGenerationAlgorithm, int keySize) throws GeneralSecurityException, IOException {
        KeyPair keyPair = cryptographyService.generateKeyPair(keyGenerationAlgorithm, keySize);
        return ProxyKey.builder()
                .id(ProxyId.of(id))
                .localAlias(id)
                .privateKey(keyPair.getPrivate())
                .privateKeyEncoded(pemService.encodePrivateKey(keyPair.getPrivate()))
                .publicKey(keyPair.getPublic())
                .publicKeyEncoded(pemService.encodePublicKey(keyPair.getPublic()))
                .build();
    }
}
