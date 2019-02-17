package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@ToString(of = {"id", "name"})
public class Proxy {

  @NonNull
  private ProxyId id;

  private String name;

  @NonNull
  private Certificate certificate;

  public static Proxy of(Certificate certificate) {
    return builder()
        .id(ProxyId.of(certificate))
        .name(certificate.getOwner())
        .certificate(certificate)
        .build();
  }

  @JsonIgnore
  public String getUniqueId() {
    return id.uniqueId();
  }

  @JsonIgnore
  public boolean isValid() {
    return id != null && id.isValid()
        && certificate != null && certificate.isValid();
  }
}
