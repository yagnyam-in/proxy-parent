package in.yagnyam.proxy.services;

import static java.lang.Math.toIntExact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkService {

  public static class HttpException extends Exception {

    @Getter
    private final int statusCode;

    public HttpException(int statusCode, String errorMessage) {
      super(errorMessage);
      this.statusCode = statusCode;
    }

    public static HttpException of(int statusCode, String errorMessage) {
      return new HttpException(statusCode, errorMessage);
    }
  }

  public static class HttpResponse implements AutoCloseable {

    private final String url;
    private final com.google.api.client.http.HttpResponse response;
    private String content;

    private HttpResponse(String url, com.google.api.client.http.HttpResponse response) {
      this.url = url;
      this.response = response;
    }

    public int getStatusCode() {
      return response.getStatusCode();
    }

    public String getContent() throws IOException, HttpException {
      if (content == null) {
        content = response.parseAsString();
      }
      if (response.isSuccessStatusCode()) {
        log.debug("{} => {}", url, content);
        return content;
      } else {
        log.error("Request " + url + " failed with status " + response.getStatusMessage());
        throw new HttpException(response.getStatusCode(), response.getStatusMessage());
      }
    }

    public <T> T getValue(Class<T> valueClass) throws IOException, HttpException {
      return new ObjectMapper().readValue(getContent(), valueClass);
    }

    public static HttpResponse of(String url, com.google.api.client.http.HttpResponse response) {
      return new HttpResponse(url, response);
    }

    @Override
    public void close() throws IOException {
      response.disconnect();
    }
  }

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

  public static Builder builder() {
    return new Builder();
  }

  public String get(String url) throws IOException, HttpException {
    try (HttpResponse response = HttpResponse
        .of(url, httpRequestFactory()
            .buildGetRequest(new GenericUrl(url))
            .setThrowExceptionOnExecuteError(false)
            .execute())) {
      return response.getContent();
    }
  }

  public <T> T getValue(String url, Class<T> resultClass) throws IOException, HttpException {
    return new ObjectMapper().readValue(get(url), resultClass);
  }

  public <I, O> O postValue(String url, I request, Class<O> resultClass)
      throws IOException, HttpException {
    return postValueWithHeaders(url, Collections.emptyMap(), request, resultClass);
  }

  private <T> HttpResponse postValueWithHeaders(String url, Map<String, String> headers, T request)
      throws IOException {
    byte[] requestBytes = new ObjectMapper().writeValueAsBytes(request);
    HttpContent httpContent = new ByteArrayContent(MediaType.JSON_UTF_8.toString(), requestBytes);
    return HttpResponse.of(url, httpRequestFactory(headers)
        .buildPostRequest(new GenericUrl(url), httpContent)
        .setLoggingEnabled(true)
        .setSuppressUserAgentSuffix(true)
        .setThrowExceptionOnExecuteError(false)
        .execute());
  }

  public <I, O> O postValueWithHeaders(String url, Map<String, String> headers, I request,
      Class<O> resultClass) throws IOException, HttpException {
    try (HttpResponse httpResponse = postValueWithHeaders(url, headers, request)) {
      return httpResponse.getValue(resultClass);
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
      additionalHeaders
          .forEach((h, v) -> request.getHeaders().set(h, Collections.singletonList(v)));
      request.setConnectTimeout(connectionTimeout);
      request.setReadTimeout(readTimeout);
    });
  }

  public static class Builder {

    private long connectionTimeout = TimeUnit.SECONDS.toMillis(60);
    private long readTimeout = TimeUnit.SECONDS.toMillis(60);
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
