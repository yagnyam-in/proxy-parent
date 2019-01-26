package in.yagnyam.proxy;

import in.yagnyam.proxy.utils.StringUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
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

  public static Proxy of(Certificate certificate) {
    return builder()
        .id(ProxyId.of(certificate))
        .certificateSerialNumber(certificate.getSerialNumber())
        .name(certificate.getOwner())
        .certificate(certificate)
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
