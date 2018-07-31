package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateChain {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String certificateSerial;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String certificateId;

  @Singular
  private List<Certificate> certificates;

  @JsonIgnore
  public Optional<Certificate> getCertificateWithId() {
    return certificates.stream().filter(c -> c.matchesId(certificateId)).findFirst();
  }

  @JsonIgnore
  public Optional<Certificate> getCertificateWithSerial() {
    return certificates.stream().filter(c -> c.getSerialNumber().equals(certificateSerial))
        .findFirst();
  }

  @JsonIgnore
  public boolean isValid() {
    return (StringUtils.isValid(certificateSerial) || StringUtils.isValid(certificateId))
        && certificates.stream().allMatch(Certificate::isValid);
  }
}
