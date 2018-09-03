package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
public class SignedMessageSignature {

  private String algorithm;

  private String value;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(algorithm) && StringUtils.isValid(value);
  }

}
