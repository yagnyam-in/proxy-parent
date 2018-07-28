package in.yagnyam.proxy.messages.identity;

import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;

/**
 * Identity Subject to represent Person
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"subjectId"})
public class IdentitySubject {

    /**
     * Subject Unique Id
     */
    private String subjectId;

    private String nationality;

    private String name;

    private String gender;

    private Date dateOfBirth;

    public boolean isValid() {
        return StringUtils.isValid(subjectId);
    }
}
