package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonInclude;

import in.yagnyam.proxy.CertificateChain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Registration Response
 */
@Getter
@ToString
@EqualsAndHashCode(of = "requestId", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class CustomerRegistrationResponse {

    @NonNull
    private String requestId;
    
    @NonNull
    private String proxyId;

    @NonNull
    private String certificateSerial;

    @NonNull
    private CertificateChain certificateChain;
}
