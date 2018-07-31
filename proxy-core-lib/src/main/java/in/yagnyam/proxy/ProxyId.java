package in.yagnyam.proxy;

import in.yagnyam.proxy.utils.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = {"id", "sha256Thumbprint"})
public class ProxyId {

  private final String id;
  private final String sha256Thumbprint;
  private final String uniqueId;

  private ProxyId(String id, String sha256Thumbprint) {
    if (StringUtils.isEmpty(id)) {
      throw new IllegalArgumentException("Invalid Proxy Id: " + id);
    }
    this.id = id;
    this.sha256Thumbprint = sha256Thumbprint;
    this.uniqueId = uniqueId();
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

  private String uniqueId() {
    if (StringUtils.isValid(sha256Thumbprint)) {
      return id + "#" + sha256Thumbprint;
    } else {
      return id;
    }
  }

  public boolean isValid() {
    return StringUtils.isValid(id);
  }

  @Override
  public String toString() {
    return uniqueId;
  }

}
