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
public class ProxyId implements ProxyBaseObject {

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

  public static ProxyId any() {
    return of("any", "any");
  }

  @JsonIgnore
  public boolean canSignOnBehalfOf(ProxyId other) {
    if (other == null) {
      return false;
    }
    if (other.equals(ProxyId.any())) {
      return true;
    }
    if (other.id.equals(id) && other.sha256Thumbprint == null) {
      return true;
    }
    return equals(other);
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
  @Override
  public boolean isValid() {
    return StringUtils.isValid(id);
  }

  @Override
  public String toString() {
    return uniqueId();
  }

}
