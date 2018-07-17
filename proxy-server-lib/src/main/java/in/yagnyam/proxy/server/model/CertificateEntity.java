package in.yagnyam.proxy.server.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "serialNumber")
public class CertificateEntity {

    @Id
    private String serialNumber;

    @Index
    private String owner;

    @Index
    private String sha256Thumbprint;

    private String alias;

    private String subject;

    private Date validFrom;

    private Date validTill;

    private String certificateEncoded;

}
