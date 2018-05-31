package in.yagnyam.digana.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import in.yagnyam.proxy.Certificate;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "serialNumber")
public class CertificateEntity {

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

}
