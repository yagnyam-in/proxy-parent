package in.yagnyam.proxy.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Builder
public class MessageSerializerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T extends SignableMessage> String serialize(SignedMessage<T> message) throws IOException {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize " + message, e);
            throw new IOException(e);
        }
    }

    public String serialize(SignableMessage message) throws IOException {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize " + message, e);
            throw new IOException(e);
        }
    }

    public String serialize(Object object) throws IOException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize " + object, e);
            throw new IOException(e);
        }
    }

    public <T> T deserialize(String message, Class<T> tClass) throws IOException {
        try {
            return objectMapper.readValue(message, tClass);
        } catch (IOException e) {
            log.error("Unable to deserialize " + message);
            throw e;
        }
    }

}
