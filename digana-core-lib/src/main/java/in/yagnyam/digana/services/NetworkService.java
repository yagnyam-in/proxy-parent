package in.yagnyam.digana.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.net.MediaType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.toIntExact;

@Slf4j
public class NetworkService {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private final Map<String, String> defaultHeaders;
    private final int connectionTimeout;
    private final int readTimeout;

    private NetworkService(@NonNull Map<String, String> defaultHeaders,
                           int connectionTimeout, int readTimeout) {
        this.defaultHeaders = defaultHeaders;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public String get(String url) throws IOException {
        HttpResponse httpResponse = httpRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
        return extractResponse(url, httpResponse);
    }

    public <T> T getValue(String url, Class<T> resultClass) throws IOException {
        return new ObjectMapper().readValue(get(url), resultClass);
    }

    public <O> O postJsonString(String url, Map<String, String> headers, String request, Class<O> resultClass) throws IOException {
        log.debug("POST {} with {}", url, request);
        HttpContent httpContent = ByteArrayContent.fromString(MediaType.JSON_UTF_8.toString(), request);
        HttpResponse httpResponse = httpRequestFactory(headers)
                .buildPostRequest(new GenericUrl(url), httpContent)
                .setLoggingEnabled(true)
                .setSuppressUserAgentSuffix(true)
                .execute();
        String response = extractResponse(url, httpResponse);
        log.info("POST {} with {} => {}", url, request, response);
        return new ObjectMapper().readValue(response, resultClass);
    }


    public <I, O> O postValue(String url, Map<String, String> headers, I request, Class<O> resultClass) throws IOException {
        log.debug("POST {} with {}", url, request);
        byte[] requestBytes = new ObjectMapper().writeValueAsBytes(request);
        HttpContent httpContent = new ByteArrayContent(MediaType.JSON_UTF_8.toString(), requestBytes);
        HttpResponse httpResponse = httpRequestFactory(headers)
                .buildPostRequest(new GenericUrl(url), httpContent)
                .setLoggingEnabled(true)
                .setSuppressUserAgentSuffix(true)
                .execute();
        String response = extractResponse(url, httpResponse);
        log.info("POST {} with {} => {}", url, request, response);
        return new ObjectMapper().readValue(response, resultClass);
    }

    private String extractResponse(String url, HttpResponse httpResponse) throws IOException {
        // TODO: Is this too broad ??
        if (httpResponse.isSuccessStatusCode()) {
            String response = httpResponse.parseAsString();
            log.debug("{} => {}", url, response);
            return response;
        } else {
            log.error("Request " + url + " failed with status " + httpResponse.getStatusMessage());
            throw new IOException("Request " + url + " failed with status " + httpResponse.getStatusMessage());
        }
    }

    private HttpRequestFactory httpRequestFactory() {
        return HTTP_TRANSPORT.createRequestFactory(request -> {
            defaultHeaders.forEach((h, v) -> request.getHeaders().set(h, v));
            request.setConnectTimeout(connectionTimeout);
            request.setReadTimeout(readTimeout);
        });
    }

    private HttpRequestFactory httpRequestFactory(Map<String, String> additionalHeaders) {
        return HTTP_TRANSPORT.createRequestFactory(request -> {
            defaultHeaders.forEach((h, v) -> request.getHeaders().set(h, Collections.singletonList(v)));
            additionalHeaders.forEach((h, v) -> request.getHeaders().set(h, Collections.singletonList(v)));
            request.setConnectTimeout(connectionTimeout);
            request.setReadTimeout(readTimeout);
        });
    }




    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private long connectionTimeout = TimeUnit.SECONDS.toMillis(30);
        private long readTimeout = TimeUnit.SECONDS.toMillis(30);
        private Map<String, String> headers = new HashMap<>();

        private Builder() {
        }

        public Builder readTimeout(int timeout, TimeUnit unit) {
            this.readTimeout = unit.toMillis(timeout);
            return this;
        }

        public Builder connectionTimeout(int timeout, TimeUnit unit) {
            this.connectionTimeout = unit.toMillis(timeout);
            return this;
        }

        public Builder header(String header, String value) {
            this.headers.put(header, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public NetworkService build() {
            return new NetworkService(headers, toIntExact(connectionTimeout), toIntExact(readTimeout));
        }
    }


}
