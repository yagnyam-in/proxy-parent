package in.yagnyam.digana.authentication;

import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.Map;


@Builder
@Getter
@ToString
public class AuthenticationToken {

    @NonNull
    private final String keyId;

    @NonNull
    private final String issuer;

    @Singular
    @NonNull
    private final List<String> audiences;

    @NonNull
    private final String subject;

    /**
     * Only till seconds. Milliseconds are ignored
     */
    @NonNull
    private Date expirationTime;

    @Singular
    private Map<String, String> stringAttributes;

    @Singular
    private Map<String, List<String>> stringListAttributes;

    @Singular
    private Map<String, Date> dateAttributes;

}
