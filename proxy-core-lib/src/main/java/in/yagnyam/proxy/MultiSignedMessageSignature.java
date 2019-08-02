package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.List;


@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class MultiSignedMessageSignature implements ProxyBaseObject {

    @NonNull
    private ProxyId signedBy;

    @NonNull
    @Singular
    private List<SignedMessageSignature> signatures;

    @JsonIgnore
    public boolean isValid() {
        return signedBy != null && signedBy.isValid()
                && signatures != null && signatures.stream().allMatch(SignedMessageSignature::isValid);
    }

}
