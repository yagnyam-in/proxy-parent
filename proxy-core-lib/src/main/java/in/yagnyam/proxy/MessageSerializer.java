package in.yagnyam.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class MessageSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper() {{
    }};

    public String serialize(SignedMessage message) throws IOException {
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

    public <T> T deserialize(String message, Class<T> tClass) throws IOException {
        try {
            return objectMapper.readValue(message, tClass);
        } catch (IOException e) {
            log.error("Unable to deserialize " + message);
            throw e;
        }
    }

}
