package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
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
