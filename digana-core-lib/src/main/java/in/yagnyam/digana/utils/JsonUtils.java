package in.yagnyam.digana.utils;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;

/**
 * Json Utility methods
 */
public class JsonUtils {

    public static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    @SneakyThrows(IOException.class)
    public static String toJson(Object o) {
        return getObjectMapper().writeValueAsString(o);
    }

    @SneakyThrows(IOException.class)
    public static <T> T fromJson(String jsonString, Class<T> destinationClass) {
        return getObjectMapper().readValue(jsonString, destinationClass);
    }

    @SneakyThrows(IOException.class)
    public static <T> T convert(Object from, Class<T> destinationClass) {
        return getObjectMapper().readValue(toJson(from), destinationClass);
    }

}

