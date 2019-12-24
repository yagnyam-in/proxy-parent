package in.yagnyam.proxy.bootstrap;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyRequest;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.services.MessageFactory;
import in.yagnyam.proxy.services.MessageSerializerService;
import in.yagnyam.proxy.services.NetworkService;
import lombok.Builder;
import lombok.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;

@Builder
public class ProxyFactory {

    @Builder.Default
    private String createProxyUrl = "https://cs.pxy.yagnyam.in/proxy";

    @NonNull
    private NetworkService networkService;

    @NonNull
    private MessageFactory messageFactory;

    @NonNull
    private MessageSerializerService messageSerializerService;

    @NonNull
    public Proxy createProxy(ProxyRequest proxyRequest) throws GeneralSecurityException, IOException {
        ProxyCreationRequest request = ProxyCreationRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .proxyId(proxyRequest.getId())
                .certificateRequestEncoded(proxyRequest.getRequestEncoded())
                .revocationPassPhraseHash(proxyRequest.getRevocationPassPhraseHash())
                .build();
        String rawResponse = extractContent(networkService.postValueWithHeaders(createProxyUrl, Collections.emptyMap(), request));
        try {
            return messageSerializerService.deserializeSignableMessage(rawResponse, ProxyCreationResponse.class).getProxy();
        } catch (IOException e) {
            SignedMessage<ProxyCreationResponse> proxyCreationResponse = messageFactory.buildAndVerifySignedMessage(rawResponse, ProxyCreationResponse.class);
            return proxyCreationResponse.getMessage().getProxy();
        }
    }

    private String extractContent(NetworkService.HttpResponse httpResponse) throws IOException {
        try {
            return httpResponse.getContent();
        } catch (NetworkService.HttpException e) {
            throw new IOException(e);
        }
    }


}
