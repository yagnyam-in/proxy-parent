package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CipherText implements ProxyBaseObject {

    // IV is optional for Asymmetric key encryption (e.g. RSA)
    private String iv;

    @NonNull
    private String algorithm;

    @NonNull
    private String cipherText;

    private String hmacAlgorithm;

    private String hmac;

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(algorithm) && StringUtils.isValid(cipherText);
    }

}
