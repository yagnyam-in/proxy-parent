package in.yagnyam.digana.customers;

import lombok.*;

import java.util.Map;

/**
 * Customer Update Request
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "requestNumber")
public class CustomerUpdateRequest {

    @NonNull
    private String requestNumber;

    private String name;

    private String phoneNumber;

    private String emailAddress;

    private String gcmToken;

    private String bleUUID;

    private Boolean syncWithContacts;

    private PreferredAccount preferredAccount;

}
