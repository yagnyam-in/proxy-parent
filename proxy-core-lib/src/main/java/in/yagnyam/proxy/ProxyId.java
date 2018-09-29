package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
  public boolean isParentOrEqualsOf(ProxyId proxyId) {
    return Objects.equals(id, proxyId.id) &&
        (sha256Thumbprint == null || sha256Thumbprint.equals(proxyId.sha256Thumbprint));
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
