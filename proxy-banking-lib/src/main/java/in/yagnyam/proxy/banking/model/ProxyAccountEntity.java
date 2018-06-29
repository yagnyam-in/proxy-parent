package in.yagnyam.proxy.banking.model;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;
import in.yagnyam.proxy.messages.banking.*;
import lombok.*;

import java.util.Date;

/**
 * Entity to represent Proxy Account
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "proxyAccountId")
public class ProxyAccountEntity {

    @Id
    @NonNull
    private String proxyAccountId;

    @NonNull
    @Index
    private String proxyId;

    @Index
    @NonNull
    private String originalAccountId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    /**
     * Maximum amount for which *each* Payment can be made
     */
    @NonNull
    private Amount maximumAmountPerTransaction;

    @Load
    private Ref<OriginalAccountEntity> originalAccountEntityRef;

    @Load
    private Ref<AccountCredentialsEntity> accountCredentialsEntityRef;

    public OriginalAccountEntity getOriginalAccountEntity() {
        return originalAccountEntityRef.get();
    }

    public void setOriginalAccountEntity(OriginalAccountEntity originalAccountEntity) {
        this.originalAccountEntityRef = Ref.create(originalAccountEntity);
    }

    public AccountCredentialsEntity getAccountCredentialsEntity() {
        return accountCredentialsEntityRef.get();
    }

    public void setAccountCredentialsEntity(AccountCredentialsEntity accountCredentialsEntity) {
        this.accountCredentialsEntityRef = Ref.create(accountCredentialsEntity);
    }
}
