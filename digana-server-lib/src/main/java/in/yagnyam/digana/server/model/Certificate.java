package in.yagnyam.digana.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import in.yagnyam.digana.utils.StringUtils;
import lombok.*;

import java.security.cert.X509Certificate;
import java.util.Date;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Certificate {

    public static final String OWNER = "owner";

    @Id
    private String serialNumber;

    @Index
    private String owner;

    private String sha1Thumbprint;

    @Index
    private String sha256Thumbprint;

    private String alias;

    private String subject;

    private Date validFrom;

    private Date validTill;

    private String certificateEncoded;

    @Ignore
    @JsonIgnore
    @Setter
    private X509Certificate certificate;

    @Override
    public int hashCode() {
        return StringUtils.hashCode(serialNumber);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Certificate)
                && StringUtils.equals(((Certificate) o).serialNumber, serialNumber);
    }
}
