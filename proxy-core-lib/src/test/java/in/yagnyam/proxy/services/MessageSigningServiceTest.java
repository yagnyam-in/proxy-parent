package in.yagnyam.proxy.services;

import static org.mockito.Mockito.mock;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import lombok.SneakyThrows;
import lombok.val;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageSigningServiceTest {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private CryptographyService cryptographyService = BcCryptographyService.builder().build();

  @SneakyThrows
  private PrivateKey samplePrivateKey() {
    KeyPairGenerator generator = KeyPairGenerator
        .getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
    generator.initialize(2048, new SecureRandom());
    return generator.generateKeyPair().getPrivate();
  }

  @Test
  public void testSign() throws IOException, GeneralSecurityException {
    ProxyId proxyId = ProxyId.of("dummy", "SHA256");
    Proxy proxy = Proxy.builder()
        .id(proxyId)
        .certificateSerialNumber("1234")
        .privateKey(samplePrivateKey())
        .certificate(mock(Certificate.class)).build();

    SignableMessage signableMessage = new SignableMessage() {
      @Override
      public ProxyId signer() {
        return ProxyId.of("dummy", "SHA256");
      }

      @Override
      public String toReadableString() {
        return "dummy";
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
    MessageSigningService service = MessageSigningService.builder()
        .serializer(new MessageSerializerService())
        .cryptographyService(cryptographyService)
        .signatureAlgorithm("SHA256WithRSAEncryption")
        .build();
    val s = service.sign(signableMessage, proxy);
    System.out.println(s);
  }


  @Test(expected = IllegalStateException.class)
  public void testSignWithDifferentSha() throws IOException, GeneralSecurityException {
    ProxyId proxyId = ProxyId.of("dummy", "DifferentSha256");
    Proxy proxy = Proxy.builder()
        .id(proxyId)
        .certificateSerialNumber("123456")
        .privateKey(samplePrivateKey())
        .certificate(mock(Certificate.class)).build();

    SignableMessage signableMessage = new SignableMessage() {
      @Override
      public ProxyId signer() {
        return ProxyId.of("dummy", "SHA256");
      }

      @Override
      public String toReadableString() {
        return "dummy";
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
    MessageSigningService service = MessageSigningService.builder()
        .serializer(new MessageSerializerService())
        .cryptographyService(cryptographyService)
        .signatureAlgorithm("SHA256WithRSAEncryption")
        .build();
    val s = service.sign(signableMessage, proxy);
    System.out.println(s);
  }

  @Test
  public void testSignForProxyIdWithoutSha() throws IOException, GeneralSecurityException {
    ProxyId proxyId = ProxyId.of("dummy", "DifferentSha256");
    Proxy proxy = Proxy.builder()
        .id(proxyId)
        .certificateSerialNumber("12345")
        .privateKey(samplePrivateKey())
        .certificate(mock(Certificate.class)).build();

    SignableMessage signableMessage = new SignableMessage() {
      @Override
      public ProxyId signer() {
        return ProxyId.of("dummy");
      }

      @Override
      public String toReadableString() {
        return "dummy";
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
    MessageSigningService service = MessageSigningService.builder()
        .serializer(new MessageSerializerService())
        .cryptographyService(cryptographyService)
        .signatureAlgorithm("SHA256WithRSAEncryption")
        .build();
    val s = service.sign(signableMessage, proxy);
    System.out.println(s);
  }


}
