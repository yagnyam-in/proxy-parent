package in.yagnyam.proxy.authentication;

import in.yagnyam.proxy.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Slf4j
public class AuthenticationTokenTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testConstruction_NoKeyId() {
        exception.expect(NullPointerException.class);
        AuthenticationToken.builder()
                .audience("To")
                .issuer("From")
                .subject("Subject")
                .build();
    }


    @Test
    public void testConstruction_NoIssuer() {
        exception.expect(NullPointerException.class);
        AuthenticationToken.builder()
                .keyId("Kid")
                .audience("To")
                .subject("Subject")
                .build();
    }


    @Test
    public void testConstruction_NoSubject() {
        exception.expect(NullPointerException.class);
        AuthenticationToken.builder()
                .keyId("Kid")
                .audience("To")
                .issuer("From")
                .build();
    }


    @Test
    public void testConstruction_NoExpiration() {
        exception.expect(NullPointerException.class);
        AuthenticationToken.builder()
                .keyId("Kid")
                .audience("To")
                .issuer("From")
                .subject("Subject")
                .build();
    }


    @Test
    public void testConstruction() {
        AuthenticationToken.builder()
                .keyId("Kid")
                .audience("To")
                .issuer("From")
                .subject("Subject")
                .expirationTime(DateUtils.afterYears(1))
                .build();
    }
}
