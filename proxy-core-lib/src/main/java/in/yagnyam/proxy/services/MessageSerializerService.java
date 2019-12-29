package in.yagnyam.proxy.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.yagnyam.proxy.MultiSignableMessage;
import in.yagnyam.proxy.MultiSignedMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("unchecked")
public class MessageSerializerService {

  protected ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return objectMapper;
  }

  public <T extends SignableMessage> String serializeSignedMessage(SignedMessage<T> message)
      throws IOException {
    try {
      return objectMapper().writeValueAsString(message);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize Signed Message " + message, e);
      throw new IOException(e);
    }
  }

  public String serializeSignableMessage(SignableMessage message) throws IOException {
    try {
      return objectMapper().writeValueAsString(message);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize Signable Message " + message, e);
      throw new IOException(e);
    }
  }


  public <T extends MultiSignableMessage> String serializeMultiSignedMessage(MultiSignedMessage<T> message)
      throws IOException {
    try {
      return objectMapper().writeValueAsString(message);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize MultiSigned Message " + message, e);
      throw new IOException(e);
    }
  }

  public String serializeMultiSignableMessage(MultiSignableMessage message) throws IOException {
    try {
      return objectMapper().writeValueAsString(message);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize MultiSignable Message " + message, e);
      throw new IOException(e);
    }
  }

  public String serializeMessage(Object object) throws IOException {
    try {
      return objectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize Message " + object, e);
      throw new IOException(e);
    }
  }


  public byte[] serializeMessageAsBytes(Object object) throws IOException {
    try {
      return objectMapper().writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize Message " + object, e);
      throw new IOException(e);
    }
  }

  public SignedMessage deserializeSignedMessage(String message)
      throws IOException {
    try {
      return objectMapper().readValue(message, SignedMessage.class);
    } catch (IOException e) {
      log.error("Unable to deserialize Signed Message: " + message, e);
      throw e;
    }
  }

  public <T extends SignableMessage> T deserializeSignableMessage(String message, Class<T> tClass)
      throws IOException {
    try {
      return objectMapper().readValue(message, tClass);
    } catch (IOException e) {
      log.error("Unable to deserialize signable Message: " + message, e);
      throw e;
    }
  }

  public MultiSignedMessage deserializeMultiSignedMessage(String message)
      throws IOException {
    try {
      return objectMapper().readValue(message, MultiSignedMessage.class);
    } catch (IOException e) {
      log.error("Unable to deserialize Signed Message: " + message, e);
      throw e;
    }
  }

  public <T extends MultiSignableMessage> T deserializeMultiSignableMessage(String message, Class<T> tClass)
      throws IOException {
    try {
      return objectMapper().readValue(message, tClass);
    } catch (IOException e) {
      log.error("Unable to deserialize signable Message: " + message, e);
      throw e;
    }
  }


  public <T> T deserializeMessage(String message, Class<T> tClass)
      throws IOException {
    try {
      return objectMapper().readValue(message, tClass);
    } catch (IOException e) {
      log.error("Unable to deserialize Message: " + message, e);
      throw e;
    }
  }


}
