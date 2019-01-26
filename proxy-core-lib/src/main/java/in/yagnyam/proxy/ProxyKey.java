package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import java.security.PrivateKey;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@ToString(of = {"id", "name"})
public class ProxyKey {

  @NonNull
  private ProxyId id;

  private String name;

  @NonNull
  private String privateKeyEncoded;

  @JsonIgnore
  @Setter
  private PrivateKey privateKey;

  public String getUniqueId() {
    return id.uniqueId();
  }

  public boolean isValid() {
    return id != null && id.isValid()
        && StringUtils.isValid(privateKeyEncoded);
  }
}
