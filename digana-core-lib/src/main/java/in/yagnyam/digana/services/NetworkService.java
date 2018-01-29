package in.yagnyam.digana.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.toIntExact;

@Slf4j
public class NetworkService {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private final HttpRequestFactory httpRequestFactory;

    private NetworkService(@NonNull Map<String, String> headers,
                           int connectionTimeout, int readTimeout) {
        this.httpRequestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
            headers.forEach((h, v) -> request.getHeaders().set(h, v));
            request.setConnectTimeout(connectionTimeout);
            request.setReadTimeout(readTimeout);
        });
    }

    public InputStream get(String url) throws IOException {
        HttpResponse response = httpRequestFactory.buildGetRequest(new GenericUrl(url)).execute();
        // TODO: Is this too broad ??
        if (response.isSuccessStatusCode()) {
            return response.getContent();
        } else {
            log.error("Request " + url + " failed with status " + response.getStatusMessage());
            throw new IOException("Request " + url + " failed with status " + response.getStatusMessage());
        }
    }

    public <T> T getValue(String url, Class<T> resultClass) throws IOException {
        return new ObjectMapper().readValue(get(url), resultClass);
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
