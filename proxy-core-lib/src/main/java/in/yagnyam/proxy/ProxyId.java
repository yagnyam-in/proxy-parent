package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProxyId proxyId = (ProxyId) o;
    return Objects.equals(id, proxyId.id)
        && (StringUtils.isEmpty(sha256Thumbprint)
        || StringUtils.isEmpty(proxyId.sha256Thumbprint)
        || sha256Thumbprint.equals(proxyId.sha256Thumbprint));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
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
