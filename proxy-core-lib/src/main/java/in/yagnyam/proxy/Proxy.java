package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.security.PrivateKey;

@Builder
@Getter
@ToString(of = {"id", "name"})
public class Proxy {

    @NonNull
    private ProxyId id;

    private String name;

    private String certificateSerialNumber;

    @NonNull
    private Certificate certificate;

    @JsonIgnore
    @Setter
    private PrivateKey privateKey;

    public String getUniqueId() {
        return id.getUniqueId();
    }

    public static Proxy of(Certificate certificate) {
        return of(certificate, null);
    }

    public static Proxy of(Certificate certificate, PrivateKey privateKey) {
        return builder()
                .id(ProxyId.of(certificate))
                .certificateSerialNumber(certificate.getSerialNumber())
                .name(certificate.getOwner())
                .certificate(certificate)
                .privateKey(privateKey)
                .build();
    }
}
