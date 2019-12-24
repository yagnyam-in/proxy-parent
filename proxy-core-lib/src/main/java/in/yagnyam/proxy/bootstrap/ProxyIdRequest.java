package in.yagnyam.proxy.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.RequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Request to get PID
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxyIdRequest implements RequestMessage {

    /**
     * Unique Request Number. No two requests shall have same request number
     */
    @NonNull
    private String requestId;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId);
    }
}
