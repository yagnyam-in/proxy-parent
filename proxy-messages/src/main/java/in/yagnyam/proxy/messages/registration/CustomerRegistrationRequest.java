package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(of = "requestId", callSuper = false)
public class CustomerRegistrationRequest {

  @NonNull
  private String requestId;

  @NonNull
  private String proxyId;

  @NonNull
  private String certificateRequestEncoded;

  private String certificateName;

  private String gcmToken;

  private String name;

  private String emailAddress;

  private String phoneNumber;

  private boolean syncWithContacts;

}
