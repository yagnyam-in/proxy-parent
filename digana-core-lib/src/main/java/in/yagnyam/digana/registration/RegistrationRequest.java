package in.yagnyam.digana.registration;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
public class RegistrationRequest {

    @NonNull
    private String requestNumber;

    @NonNull
    private String customerNumber;

    @NonNull
    private String certificationRequestEncoded;

    private String gcmToken;

    private String name;

    private String emailAddress;

    private String postalAddress;

    private String phoneNumber;

    private boolean syncWithContacts;

}
