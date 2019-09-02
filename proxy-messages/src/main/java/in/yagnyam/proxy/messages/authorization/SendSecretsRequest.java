package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.*;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SendSecretsRequest implements SignableMessage, AddressableMessage {

    @Getter
    @Builder
    @ToString(exclude = "secret")
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SecretForEmailRecipient implements ProxyBaseObject {
        @NonNull
        private String email;

        @NonNull
        private String secret;

        @NonNull
        private Hash secretHash;

        @Override
        public boolean isValid() {
            return StringUtils.isValid(email)
                    && StringUtils.isValid(secret)
                    && secretHash != null && secretHash.isValid();
        }
    }

    @Getter
    @Builder
    @ToString(exclude = "secret")
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SecretForPhoneNumberRecipient implements ProxyBaseObject {
        @NonNull
        private String phoneNumber;

        @NonNull
        private String secret;

        @NonNull
        private Hash secretHash;

        @Override
        public boolean isValid() {
            return StringUtils.isValid(phoneNumber)
                    && StringUtils.isValid(secret)
                    && secretHash != null && secretHash.isValid();
        }
    }

    @NonNull
    private ProxyId senderProxyId;

    @NonNull
    private ProxyId routerProxyId;

    @NonNull
    @Singular
    private List<SecretForEmailRecipient> secretsForEmailRecipients;

    @NonNull
    @Singular
    private List<SecretForPhoneNumberRecipient> secretsForPhoneNumberRecipients;

    @NonNull
    private Date validFrom;

    @NonNull
    private Date validTill;

    @Override
    public ProxyId address() {
        return routerProxyId;
    }

    @Override
    public ProxyId signer() {
        return senderProxyId;
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(senderProxyId)
                && ProxyUtils.isValid(routerProxyId)
                && (secretsForEmailRecipients.isEmpty() || secretsForEmailRecipients.stream().allMatch(SecretForEmailRecipient::isValid))
                && (secretsForPhoneNumberRecipients.isEmpty() || secretsForPhoneNumberRecipients.stream().allMatch(SecretForPhoneNumberRecipient::isValid))
                && DateUtils.isValid(validFrom)
                && DateUtils.isValid(validTill);

    }

}
