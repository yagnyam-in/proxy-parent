package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubjectIdentity implements SignableMessage {

    @NonNull
    private ProxyId identityProviderProxyId;

    @NonNull
    private SubjectIdTypeEnum subjectIdType;

    @NonNull
    private String subjectId;

    @NonNull
    private ProxyId ownerProxyId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @NonNull
    private SubjectDetails subjectDetails;

    @Override
    public ProxyId signer() {
        return identityProviderProxyId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(identityProviderProxyId)
                && subjectIdType != null
                && StringUtils.isValid(subjectId)
                && ProxyUtils.isValid(ownerProxyId)
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate)
                && ProxyUtils.isValid(subjectDetails);
    }
}
