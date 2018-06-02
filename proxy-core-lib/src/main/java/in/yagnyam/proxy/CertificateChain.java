package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateChain {

    @NonNull
    private String certificateSerial;

    @Singular
    private List<Certificate> certificates;

    public Certificate getCertificate() {
        return certificates.stream()
                .filter(c -> c.getSerialNumber().equals(certificateSerial))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Certificate found for serial " + certificateSerial));
    }

}
