package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

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

    @NonNull
    private Certificate certificate;

    @JsonIgnore
    private PrivateKey privateKey;

    public static Proxy of(Certificate certificate) {
        return builder()
                .id(certificate.getId())
                .sha256Thumbprint(certificate.getSha256Thumbprint())
                .name(certificate.getOwner())
                .certificate(certificate)
                .build();
    }
}
