package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.security.PrivateKey;

@Builder
@Getter
@ToString(of = {"id", "name"})
public class Proxy {

    @NonNull
    private String id;

    @NonNull
    private String sha256Thumbprint;

    private String name;

    private String certificateSerialNumber;

    @NonNull
    private Certificate certificate;

    @JsonIgnore
    @Setter
    private PrivateKey privateKey;

    public String getUniqueId() {
        return id + "#" + sha256Thumbprint;
    }

    public static Proxy of(Certificate certificate) {
        return of(certificate, null);
    }

    public static Proxy of(Certificate certificate, PrivateKey privateKey) {
        return builder()
                .id(certificate.getId())
                .sha256Thumbprint(certificate.getSha256Thumbprint())
                .certificateSerialNumber(certificate.getSerialNumber())
                .name(certificate.getOwner())
                .certificate(certificate)
                .privateKey(privateKey)
                .build();
    }
}
