package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Account Number class to represent an Account
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProxyAccountId {

    @NonNull
    private String bankId;

    @NonNull
    private String accountId;

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(bankId) && StringUtils.isValid(accountId);
    }
}
