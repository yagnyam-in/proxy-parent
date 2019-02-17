package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.StringUtils;
import java.security.cert.X509Certificate;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "certificateEncoded")
@EqualsAndHashCode(of = "serialNumber")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Certificate {

  @NonNull
  private String serialNumber;

  @NonNull
  private String owner;

  @NonNull
  private String sha256Thumbprint;

  private String alias;

  @NonNull
  private String subject;

  @NonNull
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private Date validFrom;

  @NonNull
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private Date validTill;

  @NonNull
  private String certificateEncoded;

  private String issuerSerialNumber;

  private String issuerSha256Thumbprint;

  @JsonIgnore
  @Setter
  private X509Certificate certificate;

  public String getId() {
    return owner;
  }

  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(serialNumber)
        && StringUtils.isValid(owner)
        && StringUtils.isValid(sha256Thumbprint)
        && StringUtils.isValid(subject)
        && DateUtils.isValid(validFrom)
        && DateUtils.isValid(validTill)
        && StringUtils.isValid(certificateEncoded);
  }
}
