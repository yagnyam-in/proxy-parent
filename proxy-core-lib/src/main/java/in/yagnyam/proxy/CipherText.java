package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CipherText implements ProxyBaseObject {

    // IV is optional for Asymmetric key encryption (e.g. RSA)
    private String iv;

    @NonNull
    private String encryptionAlgorithm;

    @NonNull
    private String cipherText;

    private String hmacAlgorithm;

    private String hmac;

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(encryptionAlgorithm) && StringUtils.isValid(cipherText);
    }

}
