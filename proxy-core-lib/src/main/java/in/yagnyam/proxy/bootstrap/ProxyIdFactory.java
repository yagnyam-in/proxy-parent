package in.yagnyam.proxy.bootstrap;

import in.yagnyam.proxy.services.MessageSerializerService;
import in.yagnyam.proxy.services.NetworkService;
import lombok.Builder;
import lombok.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Builder
public class ProxyIdFactory {

    @Builder.Default
    private String newProxyIdUrl = "https://cs.pxy.yagnyam.in/proxy-id";

    @NonNull
    private NetworkService networkService;

    @NonNull
    private MessageSerializerService messageSerializerService;

    public String newProxyId() throws IOException {
        ProxyIdRequest request = ProxyIdRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .build();
        try {
            String rawResponse = networkService.postValueWithHeaders(newProxyIdUrl, Collections.emptyMap(), request).getContent();
            return messageSerializerService.deserializeMessage(rawResponse, ProxyIdResponse.class).getProxyId();
        } catch (NetworkService.HttpException e) {
            throw new IOException(e);
        }
    }
}
