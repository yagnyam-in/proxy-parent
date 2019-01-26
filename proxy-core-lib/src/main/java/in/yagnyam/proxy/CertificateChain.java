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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String certificateSha256Thumbprint;

  @Singular
  private List<Certificate> certificates;

  @JsonIgnore
  public Optional<Certificate> getCertificate() {
    return certificates.stream()
        .filter(c -> StringUtils.isEmpty(certificateSerial) || certificateSerial
            .equals(c.getSerialNumber()))
        .filter(c -> StringUtils.isEmpty(certificateId) || certificateId.equals(c.getOwner()))
        .filter(c -> StringUtils.isEmpty(certificateSha256Thumbprint) || certificateSha256Thumbprint
            .equals(c.getSha256Thumbprint()))
        .findFirst();
  }

  @JsonIgnore
  public boolean isValid() {
    return (StringUtils.isValid(certificateSerial) || StringUtils.isValid(certificateId))
        && certificates.stream().allMatch(Certificate::isValid);
  }
}
