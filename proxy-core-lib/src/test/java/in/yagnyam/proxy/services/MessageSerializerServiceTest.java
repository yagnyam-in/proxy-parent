package in.yagnyam.proxy.services;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.junit.Test;

public class MessageSerializerServiceTest {

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @EqualsAndHashCode
  public static class SimpleClassWithDate {

    @NonNull
    private Date date;
  }

  @Test
  public void serializeMessage() throws IOException {
    MessageSerializerService serializerService = MessageSerializerService.builder().build();
    SimpleClassWithDate object = new SimpleClassWithDate(new Date(1));
    String json = serializerService.serializeMessage(object);
    assertEquals("{\"date\":\"1970-01-01T00:00:00.001+0000\"}", json);
  }

  @Test
  public void deserializeMessage() throws IOException {
    MessageSerializerService serializerService = MessageSerializerService.builder().build();
    String json = "{\"date\":\"1970-01-01T00:00:00.001+0000\"}";
    SimpleClassWithDate expected = new SimpleClassWithDate(new Date(1));
    SimpleClassWithDate actual = serializerService
        .deserializeMessage(json, SimpleClassWithDate.class);
    assertEquals(expected, actual);

  }
}