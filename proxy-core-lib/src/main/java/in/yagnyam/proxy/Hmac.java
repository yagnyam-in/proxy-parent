package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hmac implements ProxyBaseObject {

  @NonNull
  private String algorithm;

  @NonNull
  private String iv;

  @NonNull
  private String hmac;

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(iv) && StringUtils.isValid(algorithm) && StringUtils.isValid(hmac);
  }

}
