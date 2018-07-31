package in.yagnyam.proxy.messages.mobile;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Getter
public class IdentityRequest {

  public static final String REQUEST_IDENTITY_ACTION = "in.yagnyam.proxy.REQUEST_IDENTITY";

  private String requestId;

  private boolean revealNationality;

  private boolean revealName;

  private boolean revealAge;

  private boolean revealIs18Plus;

  private boolean revealDateOfBirth;

  private boolean consentToMail;

  private boolean consentToRequestPayments;

  private boolean consentToVoiceCall;

}
