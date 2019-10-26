package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxySubject implements SignableMessage {

    @NonNull
    private ProxySubjectId proxySubjectId;

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
        return proxySubjectId.getIssuerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(proxySubjectId)
                && ProxyUtils.isValid(ownerProxyId)
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate)
                && ProxyUtils.isValid(subjectDetails);
    }

    @JsonIgnore
    public ProxyId getIssuerProxyId() {
        return proxySubjectId.getIssuerProxyId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return proxySubjectId.getProxyUniverse();
    }
}
