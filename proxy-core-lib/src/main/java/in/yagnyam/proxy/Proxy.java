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
@ToString(of = {"id", "name", "certificateSerialNumber"})
public class Proxy {

  @NonNull
  private ProxyId id;

  private String name;

  @NonNull
  private String certificateSerialNumber;

  @NonNull
  private Certificate certificate;

  @JsonIgnore
  @Setter
  private PrivateKey privateKey;

  public static Proxy of(Certificate certificate) {
    return of(certificate, null);
  }

  public static Proxy of(Certificate certificate, PrivateKey privateKey) {
    return builder()
        .id(ProxyId.of(certificate))
        .certificateSerialNumber(certificate.getSerialNumber())
        .name(certificate.getOwner())
        .certificate(certificate)
        .privateKey(privateKey)
        .build();
  }

  public String getUniqueId() {
    return id.uniqueId();
  }

  public boolean isValid() {
    return id != null && id.isValid()
        && StringUtils.isValid(certificateSerialNumber)
        && certificate != null && certificate.isValid();
  }
}
