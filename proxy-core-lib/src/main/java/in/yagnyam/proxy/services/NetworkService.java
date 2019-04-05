package in.yagnyam.proxy.services;

import static java.lang.Math.toIntExact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
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

    HttpException(int statusCode, String errorMessage) {
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

    public String getContent(boolean failIfFailure) throws IOException, HttpException {
      if (content == null) {
        content = response.parseAsString();
      }
      if (failIfFailure && !response.isSuccessStatusCode()) {
        log.error("Request " + url + " failed with status " + response.getStatusMessage());
        throw new HttpException(response.getStatusCode(), response.getStatusMessage());
      } else {
        log.debug("{} => {}", url, content);
        return content;
      }
    }

    public String getContent() throws IOException, HttpException {
      return getContent(true);
    }

    public <T> T getValue(Class<T> valueClass) throws IOException, HttpException {
      return new ObjectMapper().readValue(getContent(), valueClass);
    }

    public <T> T getValue(Class<T> valueClass, boolean failIfFailure) throws IOException, HttpException {
      return new ObjectMapper().readValue(getContent(failIfFailure), valueClass);
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

  public String getWithHeaders(String url, Map<String, String> headers)
      throws IOException, HttpException {
    try (HttpResponse response = HttpResponse
        .of(url, httpRequestFactory(headers)
            .buildGetRequest(new GenericUrl(url))
            .setThrowExceptionOnExecuteError(false)
            .execute())) {
      return response.getContent();
    }
  }

  public String get(String url) throws IOException, HttpException {
    return getWithHeaders(url, Collections.emptyMap());
  }

  public <T> T getValue(String url, Class<T> resultClass) throws IOException, HttpException {
    return new ObjectMapper().readValue(getWithHeaders(url, Collections.emptyMap()), resultClass);
  }

  public <T> T getValueWithHeaders(String url, Map<String, String> headers, Class<T> resultClass)
      throws IOException, HttpException {
    return new ObjectMapper().readValue(getWithHeaders(url, headers), resultClass);
  }

  public <I, O> O postValue(String url, I request, Class<O> resultClass)
      throws IOException, HttpException {
    return postValueWithHeaders(url, Collections.emptyMap(), request, resultClass);
  }

  public <I> void postValue(String url, I request)
      throws IOException, HttpException {
    try (HttpResponse httpResponse = postValueWithHeaders(url, Collections.emptyMap(), request)) {
      if (httpResponse.getStatusCode() < 200 || httpResponse.getStatusCode() >= 300) {
        throw new HttpException(httpResponse.getStatusCode(), httpResponse.getContent());
      }
    }
  }

  private <T> HttpResponse postValueWithHeaders(String url, Map<String, String> headers, T request)
      throws IOException {
    byte[] requestBytes = new ObjectMapper().writeValueAsBytes(request);
    return postValueWithHeaders(url, headers, "application/json", requestBytes);
  }


  private HttpResponse postValueWithHeaders(String url, Map<String, String> headers, String type,
      byte[] requestBytes)
      throws IOException {
    HttpContent httpContent = new ByteArrayContent(type, requestBytes);
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
      if (httpResponse.getStatusCode() < 200 || httpResponse.getStatusCode() >= 300) {
        throw new HttpException(httpResponse.getStatusCode(), httpResponse.getContent());
      } else {
        return httpResponse.getValue(resultClass);
      }
    }
  }

  public <O> O postWithHeaders(String url, Map<String, String> headers, String type,
      byte[] requestBytes,
      Class<O> resultClass) throws IOException, HttpException {
    try (HttpResponse httpResponse = postValueWithHeaders(url, headers, type, requestBytes)) {
      if (httpResponse.getStatusCode() < 200 || httpResponse.getStatusCode() >= 300) {
        throw new HttpException(httpResponse.getStatusCode(), httpResponse.getContent());
      } else {
        return httpResponse.getValue(resultClass);
      }
    }
  }

  public <O> O postEmpty(String url, Map<String, String> headers, Class<O> resultClass)
      throws IOException, HttpException {
    try (HttpResponse response = HttpResponse
        .of(url, httpRequestFactory(headers)
            .buildPostRequest(new GenericUrl(url), new EmptyContent())
            .setThrowExceptionOnExecuteError(false)
            .execute())) {
      return response.getValue(resultClass);
    }
  }

  public HttpResponse postEmpty(String url, Map<String, String> headers)
      throws IOException, HttpException {
    return HttpResponse
        .of(url, httpRequestFactory(headers)
            .buildPostRequest(new GenericUrl(url), new EmptyContent())
            .setThrowExceptionOnExecuteError(false)
            .execute());
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
