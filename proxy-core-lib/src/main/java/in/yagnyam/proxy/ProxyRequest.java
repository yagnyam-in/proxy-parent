package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxyRequest implements ProxyBaseObject {

    @NonNull
    private String id;

    @NonNull
    private Hash revocationPassPhraseHash;

    @NonNull
    private String requestEncoded;

    @JsonIgnore
    @Override
    public boolean isValid() {
        return StringUtils.isValid(id) && ProxyUtils.isValid(revocationPassPhraseHash) && StringUtils.isValid(requestEncoded);
    }

}
