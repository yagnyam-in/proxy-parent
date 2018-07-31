package in.yagnyam.proxy.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import in.yagnyam.proxy.TestUtils;
import in.yagnyam.proxy.utils.DateUtils;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Slf4j
public class AuthenticationTokenServiceTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static void assertExpirationTimeEquals(Date expected, Date actual) {
    assertEquals(expected.getTime() / 1000, actual.getTime() / 1000);
  }

  @Test
  public void testConstructionWithoutVerificationKeyResolver() {
    thrown.expect(RuntimeException.class);
    AuthenticationTokenService.builder().build();
  }

  @Test
  public void testConstructionWithVerificationKeyResolver() {
    VerificationKeyResolver verificationKeyResolver = mock(VerificationKeyResolver.class);
    AuthenticationTokenService service = AuthenticationTokenService.builder()
        .verificationKeyResolver(verificationKeyResolver)
        .build();
    assertNotNull(service);
  }

  @Test
  public void testSignTokenAndParseToken_Simple() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    X509Certificate certificate = TestUtils.createCertificate(keyPair);
    AuthenticationToken token = AuthenticationToken.builder().
        keyId(certificate.getSerialNumber().toString())
        .audience("To")
        .issuer("From")
        .subject("Sub")
        .expirationTime(DateUtils.afterYears(1))
        .build();
    VerificationKeyResolver verificationKeyResolver = new X509VerificationKeyResolver(certificate);
    AuthenticationTokenService service = AuthenticationTokenService.builder()
        .verificationKeyResolver(verificationKeyResolver).build();
    String jwt = service.signToken(token, keyPair.getPrivate(), certificate);
    assertNotNull(jwt);
    AuthenticationToken tokenParsed = service.parseToken(jwt, "To").get();
    assertEquals(token.getAudiences(), tokenParsed.getAudiences());
    assertEquals(token.getSubject(), tokenParsed.getSubject());
    assertEquals(token.getIssuer(), tokenParsed.getIssuer());
    assertExpirationTimeEquals(token.getExpirationTime(), tokenParsed.getExpirationTime());
  }

  @Test
  public void testSignTokenAndParseToken_Cliams() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    X509Certificate certificate = TestUtils.createCertificate(keyPair);
    AuthenticationToken token = AuthenticationToken.builder()
        .keyId(certificate.getSerialNumber().toString())
        .audience("To")
        .issuer("From")
        .subject("Sub")
        .stringAttribute("hello", "there")
        .stringListAttribute("list", Collections.singletonList("one"))
        .expirationTime(DateUtils.afterYears(1))
        .build();
    VerificationKeyResolver verificationKeyResolver = new X509VerificationKeyResolver(certificate);
    AuthenticationTokenService service = AuthenticationTokenService.builder()
        .verificationKeyResolver(verificationKeyResolver).build();
    String jwt = service.signToken(token, keyPair.getPrivate(), certificate);
    assertNotNull(jwt);
    AuthenticationToken tokenParsed = service.parseToken(jwt, "To").get();
    assertEquals(token.getAudiences(), tokenParsed.getAudiences());
    assertEquals(token.getSubject(), tokenParsed.getSubject());
    assertEquals(token.getIssuer(), tokenParsed.getIssuer());
    assertExpirationTimeEquals(token.getExpirationTime(), tokenParsed.getExpirationTime());
    assertEquals("there", tokenParsed.getStringAttributes().get("hello"));
    assertEquals(token.getStringListAttributes(), tokenParsed.getStringListAttributes());
  }

}
