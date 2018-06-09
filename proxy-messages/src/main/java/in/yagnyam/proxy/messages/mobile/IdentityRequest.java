package in.yagnyam.proxy.messages.mobile;

import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Getter
public class IdentityRequest {

    private String requestId;

    private boolean revealNationality;

    private boolean revealName;

    private boolean revealAge;

    private boolean revealIs18Plus;

    private boolean revealDateOfBirth;
}
