package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

/**
 * Registration Response
 */
@Getter
@ToString
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class CustomerRegistrationResponse {

    @NonNull
    private String requestNumber;

    private String certificateSerial;

    @Singular
    private Map<String, String> certificates;

}
