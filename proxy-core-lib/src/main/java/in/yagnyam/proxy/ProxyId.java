package in.yagnyam.proxy;

import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"id", "sha256Thumbprint"})
public class ProxyId {

  private String id;
  private String sha256Thumbprint;

  private ProxyId(String id, String sha256Thumbprint) {
    if (StringUtils.isEmpty(id)) {
      throw new IllegalArgumentException("Invalid Proxy Id: " + id);
    }
    this.id = id;
    this.sha256Thumbprint = sha256Thumbprint;
  }

  public static ProxyId of(String id) {
    return new ProxyId(id, null);
  }

  public static ProxyId of(String id, String sha256Thumbprint) {
    return new ProxyId(id, sha256Thumbprint);
  }

  public static ProxyId of(Certificate certificate) {
    return of(certificate.getId(), certificate.getSha256Thumbprint());
  }

  @JsonIgnore
  public String uniqueId() {
    if (StringUtils.isValid(sha256Thumbprint)) {
      return id + "#" + sha256Thumbprint;
    } else {
      return id;
    }
  }

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(id);
  }

  @Override
  public String toString() {
    return uniqueId();
  }

}
