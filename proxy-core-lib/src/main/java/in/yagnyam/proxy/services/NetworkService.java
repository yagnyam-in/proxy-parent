package in.yagnyam.proxy.services;

import static java.lang.Math.toIntExact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
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
    HttpResponse httpResponse = httpRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
    try {
      return extractResponse(url, httpResponse);
    } finally {
      httpResponse.disconnect();
    }
  }

  public <T> T getValue(String url, Class<T> resultClass) throws IOException, HttpException {
    return new ObjectMapper().readValue(get(url), resultClass);
  }

  public <O> O postJsonString(String url, Map<String, String> headers, String request,
      Class<O> resultClass) throws IOException, HttpException {
    log.debug("POST {} with {}", url, request);
    HttpContent httpContent = ByteArrayContent.fromString(MediaType.JSON_UTF_8.toString(), request);
    HttpResponse httpResponse = httpRequestFactory(headers)
        .buildPostRequest(new GenericUrl(url), httpContent)
        .setLoggingEnabled(true)
        .setSuppressUserAgentSuffix(true)
        .execute();
    try {
      String response = extractResponse(url, httpResponse);
      log.info("POST {} with {} => {}", url, request, response);
      return new ObjectMapper().readValue(response, resultClass);
    } finally {
      httpResponse.disconnect();
    }
  }

  public <I, O> O postValue(String url, I request, Class<O> resultClass)
      throws IOException, HttpException {
    return postValueWithHeaders(url, Collections.emptyMap(), request, resultClass);
  }

  public <T> HttpResponse postValue(String url, T request) throws IOException {
    return postValueWithHeaders(url, Collections.emptyMap(), request);
  }

  public <T> HttpResponse postValueWithHeaders(String url, Map<String, String> headers, T request)
      throws IOException {
    log.debug("POST {} with {}", url, request);
    byte[] requestBytes = new ObjectMapper().writeValueAsBytes(request);
    HttpContent httpContent = new ByteArrayContent(MediaType.JSON_UTF_8.toString(), requestBytes);
    return httpRequestFactory(headers)
        .buildPostRequest(new GenericUrl(url), httpContent)
        .setLoggingEnabled(true)
        .setSuppressUserAgentSuffix(true)
        .execute();
  }

  public <I, O> O postValueWithHeaders(String url, Map<String, String> headers, I request,
      Class<O> resultClass) throws IOException, HttpException {
    HttpResponse httpResponse = postValueWithHeaders(url, headers, request);
    String response = extractResponse(url, httpResponse);
    log.info("POST {} with {} => {}", url, request, response);
    return new ObjectMapper().readValue(response, resultClass);
  }

  private String extractResponse(String url, HttpResponse httpResponse)
      throws IOException, HttpException {
    // TODO: Is this too broad ??
    if (httpResponse.isSuccessStatusCode()) {
      log.debug("{} => {}", url, httpResponse.parseAsString());
      return httpResponse.parseAsString();
    } else {
      log.error("Request " + url + " failed with status " + httpResponse.getStatusMessage());
      throw new HttpException(httpResponse.getStatusCode(), httpResponse.getStatusMessage());
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
