package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Optional;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateChain {

    @Singular
    private List<Certificate> certificates;

    /**
     * Return the Certificate matching given Id
     * @param certificateId Certificate Id/Serial Number
     * @return Matching certificate
     */
    public Optional<Certificate> getCertificate(String certificateId) {
        return certificates.stream().filter(c -> c.matchesId(certificateId)).findFirst();
    }

}
