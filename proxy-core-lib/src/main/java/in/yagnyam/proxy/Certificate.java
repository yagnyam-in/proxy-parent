package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

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
    private Date validFrom;

    @NonNull
    private Date validTill;

    @NonNull
    private String certificateEncoded;

    @JsonIgnore
    @Setter
    private X509Certificate certificate;

    public String getId() {
        return owner;
    }

    public String getUniqueId() {
        return owner + "#" + sha256Thumbprint;
    }

    public boolean matchesId(@NonNull String certificateId) {
        if (owner.equals(extractOnlyId(certificateId))) {
            Optional<String> sha256 = extractSha256Thumbprint(certificateId);
            return !sha256.isPresent() || sha256Thumbprint.equals(sha256.get());
        }
        return false;
    }

    public static String extractOnlyId(String certificateUniqueId) {
        if (StringUtils.isEmpty(certificateUniqueId)) {
            throw new IllegalArgumentException("Invalid certificate Id");
        }
        String[] tokens = certificateUniqueId.split("#");
        if (tokens.length <= 2) {
            return tokens[0];
        } else {
            throw new IllegalArgumentException("Invalid certificate Id" + certificateUniqueId);
        }
    }

    public static Optional<String> extractSha256Thumbprint(String certificateUniqueId) {
        if (StringUtils.isEmpty(certificateUniqueId)) {
            throw new IllegalArgumentException("Invalid certificate Id");
        }
        String[] tokens = certificateUniqueId.split("#");
        if (tokens.length < 2) {
            return Optional.empty();
        } else if (tokens.length == 2) {
            return Optional.of(tokens[1]);
        } else {
            throw new IllegalArgumentException("Invalid certificate Id" + certificateUniqueId);
        }
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
