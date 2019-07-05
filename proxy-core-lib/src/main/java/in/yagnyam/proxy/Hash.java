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
public class Hash {

  @NonNull
  private String algorithm;

  @NonNull
  private String iv;

  @NonNull
  private String hash;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(iv) && StringUtils.isValid(algorithm) && StringUtils.isValid(hash);
  }

}
