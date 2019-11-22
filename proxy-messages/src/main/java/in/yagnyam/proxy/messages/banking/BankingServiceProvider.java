package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.CollectionUtils;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankingServiceProvider implements SignableMessage {

    @NonNull
    private ProxyId proxyId;

    @NonNull
    private String proxyUniverse;

    @NonNull
    private List<String> supportedCurrencies;

    @NonNull
    private String apiEndpoint;

    @NonNull
    private String name;

    @Override
    public ProxyId signer() {
        return ProxyId.of("proxy-banking");
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(proxyId)
                && StringUtils.isValid(proxyUniverse)
                && CollectionUtils.isNotEmpty(supportedCurrencies)
                && StringUtils.isValid(apiEndpoint)
                && StringUtils.isValid(name);
    }

    @JsonIgnore
    public String getId() {
        return proxyId.getId();
    }

}
