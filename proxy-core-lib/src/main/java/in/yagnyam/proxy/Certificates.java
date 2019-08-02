package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
public class Certificates implements ProxyBaseObject {

  @Singular
  private List<Certificate> certificates;

  @Override
  @JsonIgnore
  public boolean isValid() {
    return certificates.stream().allMatch(Certificate::isValid);
  }
}
