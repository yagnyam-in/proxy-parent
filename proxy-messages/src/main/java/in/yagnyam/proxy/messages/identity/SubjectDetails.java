package in.yagnyam.proxy.messages.identity;

import in.yagnyam.proxy.ProxyBaseObject;
import lombok.*;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubjectDetails implements ProxyBaseObject {
    private SubjectIdTypeEnum subjectIdType;

    private String nationality;

    private String aadhaarNumber;

    private String name;

    // Already 50+. Better to be a String
    private String gender;

    private Integer age;

    private Boolean is18Plus;

    private Date dateOfBirth;

    @Override
    public boolean isValid() {
        return true;
    }
}
