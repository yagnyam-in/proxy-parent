package in.yagnyam.proxy.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import java.io.IOException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@SuppressWarnings("unchecked")
public class MessageSerializerService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public <T extends SignableMessage> String serializeSignedMessage(SignedMessage<T> message)
      throws IOException {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize SignedMessage " + message, e);
      throw new IOException(e);
    }
  }

  public String serializeSignableMessage(SignableMessage message) throws IOException {
    try {
      // objectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize SignableMessage " + message, e);
      throw new IOException(e);
    }
  }

  public String serializeMessage(Object object) throws IOException {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize Message " + object, e);
      throw new IOException(e);
    }
  }


  public <T extends SignableMessage> SignedMessage<T> deserializeSignedMessage(String message)
      throws IOException {
    try {
      return objectMapper.readValue(message, SignedMessage.class);
    } catch (IOException e) {
      log.error("Unable to deserialize " + message);
      throw e;
    }
  }

  public <T extends SignableMessage> T deserializeSignableMessage(String message, Class<T> tClass)
      throws IOException {
    try {
      // objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
      return objectMapper.readValue(message, tClass);
    } catch (IOException e) {
      log.error("Unable to deserialize " + message);
      throw e;
    }
  }

  public <T> T deserializeMessage(String message, Class<T> tClass)
      throws IOException {
    try {
      return objectMapper.readValue(message, tClass);
    } catch (IOException e) {
      log.error("Unable to deserialize " + message);
      throw e;
    }
  }


}
