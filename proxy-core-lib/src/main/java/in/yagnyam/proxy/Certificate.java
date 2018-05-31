package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "serialNumber")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Certificate {

    @NonNull
    private String serialNumber;

    @NonNull
    private String owner;

    private String sha1Thumbprint;

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
        return owner + "#" + sha256Thumbprint;
    }

    public boolean matchesId(@NonNull String certificateId) {
        if (owner.equals(owner(certificateId))) {
            Optional<String> sha256 = sha256Thumbprint(certificateId);
            return !sha256.isPresent() || sha256Thumbprint.equals(sha256.get());
        }
        return false;
    }

    public static String owner(@NonNull String certificateId) {
        return certificateId.split("#")[0];
    }

    public static Optional<String> sha256Thumbprint(@NonNull String certificateId) {
        String[] tokens = certificateId.split("#");
        if (tokens.length < 2) {
            return Optional.empty();
        } else if (tokens.length == 2) {
            return Optional.of(tokens[1]);
        } else {
            throw new IllegalArgumentException("Invalid Id" + certificateId);
        }
    }
}
