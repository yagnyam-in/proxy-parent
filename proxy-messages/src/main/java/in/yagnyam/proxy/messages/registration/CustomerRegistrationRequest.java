package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
public class CustomerRegistrationRequest {

    @NonNull
    private String requestNumber;

    @NonNull
    private String customerNumber;

    @NonNull
    private String certificationRequestEncoded;

    private String certificateName;

    private String gcmToken;

    private String name;

    private String emailAddress;

    private String phoneNumber;

    private boolean syncWithContacts;

}
