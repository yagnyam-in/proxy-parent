package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;


@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class SignedMessageSignature {

  @NonNull
  private String algorithm;

  @NonNull
  private String value;

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(algorithm) && StringUtils.isValid(value);
  }

}
