package in.yagnyam.digana.authentication;

import in.yagnyam.digana.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j
public class AuthenticationTokenServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        AuthenticationToken token = AuthenticationToken.builder().keyId(certificate.getSerialNumber().toString()).audience("To").issuer("From").subject("Sub").build();
        VerificationKeyResolver verificationKeyResolver = new X509VerificationKeyResolver(certificate);
        AuthenticationTokenService service = AuthenticationTokenService.builder().verificationKeyResolver(verificationKeyResolver).build();
        String jwt = service.signToken(token, keyPair.getPrivate(), certificate);
        assertNotNull(jwt);
        AuthenticationToken tokenParsed = service.parseToken(jwt, "To").get();
        assertEquals(token.getAudiences(), tokenParsed.getAudiences());
        assertEquals(token.getSubject(), tokenParsed.getSubject());
        assertEquals(token.getIssuer(), tokenParsed.getIssuer());
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
                .build();
        VerificationKeyResolver verificationKeyResolver = new X509VerificationKeyResolver(certificate);
        AuthenticationTokenService service = AuthenticationTokenService.builder().verificationKeyResolver(verificationKeyResolver).build();
        String jwt = service.signToken(token, keyPair.getPrivate(), certificate);
        assertNotNull(jwt);
        AuthenticationToken tokenParsed = service.parseToken(jwt, "To").get();
        assertEquals(token.getAudiences(), tokenParsed.getAudiences());
        assertEquals(token.getSubject(), tokenParsed.getSubject());
        assertEquals(token.getIssuer(), tokenParsed.getIssuer());
        assertEquals("there", tokenParsed.getStringAttributes().get("hello"));
        assertEquals(token.getStringListAttributes(), tokenParsed.getStringListAttributes());
    }



}
