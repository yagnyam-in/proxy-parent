package in.yagnyam.proxy.messages.payments.offline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.CollectionUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

import java.util.List;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BuyOfflineMoneyResponse implements SignableMessage {

    @NonNull
    public SignedMessage<BuyOfflineMoneyRequest> request;

    @NonNull
    private List<SignedMessage<OfflineMoney>> offlineMoney;

    @Override
    public ProxyId signer() {
        return request.getMessage().getPayerBankProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request)
                && CollectionUtils.isNotEmpty(offlineMoney)
                && offlineMoney.stream().allMatch(SignedMessage::isValid);
    }
}
