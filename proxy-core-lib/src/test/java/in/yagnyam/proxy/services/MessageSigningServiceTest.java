package in.yagnyam.proxy.services;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.SignableMessage;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;

import in.yagnyam.proxy.bootstrap.BcProxyKeyFactory;
import in.yagnyam.proxy.bootstrap.ProxyKeyFactory;
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
  private ProxyKey sampleProxyKey() {
    ProxyKeyFactory proxyKeyFactory = BcProxyKeyFactory.builder()
            .cryptographyService(BcCryptographyService.builder().build())
            .pemService(PemService.builder().build())
            .build();
    return proxyKeyFactory.createProxyKey("dummy", "RSA", 2048)
            .toBuilder()
            .id(ProxyId.of("dummy", "SHA256"))
            .build();
  }


  @Test
  public void testSign() throws IOException, GeneralSecurityException {
    ProxyKey proxyKey = sampleProxyKey();

    SignableMessage signableMessage = new SignableMessage() {
      @Override
      public ProxyId signer() {
        return ProxyId.of("dummy", "SHA256");
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
    MessageSigningService service = MessageSigningService.builder()
        .serializer(new MessageSerializerService())
        .cryptographyService(cryptographyService)
        .build();
    val s = service.singleSign(signableMessage, proxyKey);
    System.out.println(s);
  }


  @Test(expected = IllegalStateException.class)
  public void testSignWithDifferentSha() throws IOException, GeneralSecurityException {
    ProxyKey proxyKey = sampleProxyKey();
    SignableMessage signableMessage = new SignableMessage() {
      @Override
      public ProxyId signer() {
        return ProxyId.of("dummy", "DifferentSha256");
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
    MessageSigningService service = MessageSigningService.builder()
        .serializer(new MessageSerializerService())
        .cryptographyService(cryptographyService)
        .build();
    val s = service.singleSign(signableMessage, proxyKey);
    System.out.println(s);
  }

  @Test
  public void testSignForProxyIdWithoutSha() throws IOException, GeneralSecurityException {
    ProxyKey proxyKey = sampleProxyKey();
    SignableMessage signableMessage = new SignableMessage() {
      @Override
      public ProxyId signer() {
        return ProxyId.of("dummy");
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
    MessageSigningService service = MessageSigningService.builder()
        .serializer(new MessageSerializerService())
        .cryptographyService(cryptographyService)
        .build();
    val s = service.singleSign(signableMessage, proxyKey);
    System.out.println(s);
  }


}
