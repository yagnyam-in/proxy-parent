package in.yagnyam.proxy.services;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import lombok.SneakyThrows;
import lombok.ToString;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class MessageFactoryTest {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private MessageSerializerService messageSerializerService = new MessageSerializerService();

  private CryptographyService cryptographyService = BcCryptographyService.builder().build();

  private ProxyId sampleProxyId() {
    return ProxyId.of("dummy", "SHA256");
  }

  @SneakyThrows
  private ProxyKey sampleProxyKey() {
    KeyPairGenerator generator = KeyPairGenerator
        .getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
    generator.initialize(2048, new SecureRandom());
    PrivateKey privateKey = generator.generateKeyPair().getPrivate();
    return ProxyKey.builder()
        .id(sampleProxyId())
        .privateKey(privateKey)
        .privateKeyEncoded("PKE")
        .build();
  }

  @Test
  public void testVerifyAndBuildSignedMessage() throws IOException, GeneralSecurityException {
    ProxyKey proxyKey = sampleProxyKey();

    MessageVerificationService verificationService = mock(MessageVerificationService.class);
    doAnswer(invocation -> null).when(verificationService).verify(any(SignedMessage.class));

    MessageSigningService service = MessageSigningService.builder()
        .serializer(messageSerializerService)
        .cryptographyService(cryptographyService)
        .signatureAlgorithm("SHA256WithRSAEncryption")
        .build();
    SimpleSignableMessage signableMessage = new SimpleSignableMessage();
    SignedMessage<SimpleSignableMessage> signedMessage = service.sign(signableMessage, proxyKey);

    MessageFactory messageFactory = MessageFactory.builder()
        .serializer(messageSerializerService)
        .cryptographyService(cryptographyService)
        .verificationService(verificationService)
        .build();

    messageFactory.verifyAndPopulateSignedMessage(signedMessage);
  }

  @Test
  public void testVerifyAndBuildSignedMessage_InnerObject()
      throws IOException, GeneralSecurityException {
    ProxyKey proxyKey = sampleProxyKey();

    MessageVerificationService verificationService = mock(MessageVerificationService.class);
    doAnswer(invocation -> null).when(verificationService).verify(any(SignedMessage.class));

    MessageSigningService service = MessageSigningService.builder()
        .serializer(messageSerializerService)
        .cryptographyService(cryptographyService)
        .signatureAlgorithm("SHA256WithRSAEncryption")
        .build();

    SimpleSignableMessage simpleMessage = new SimpleSignableMessage();
    SignedMessage<SimpleSignableMessage> signedSimpleMessage = service
        .sign(simpleMessage, proxyKey);

    ComplexSignableMessage complexMessage = new ComplexSignableMessage();
    complexMessage.internalObject = signedSimpleMessage;
    SignedMessage<ComplexSignableMessage> signedComplexMessage = service
        .sign(complexMessage, proxyKey);
    String signedText = messageSerializerService.serializeSignedMessage(signedComplexMessage);
    System.out.println("signedText: " + signedText);

    MessageFactory messageFactory = MessageFactory.builder()
        .serializer(messageSerializerService)
        .verificationService(verificationService)
        .cryptographyService(cryptographyService)
        .build();

    SignedMessage<ComplexSignableMessage> result = messageFactory
        .buildAndVerifySignedMessage(signedText);
    System.out.println(result);
    System.out.println(result.getMessage().internalObject);
    assertNotNull(result.getMessage());
  }

  @Test
  public void testVerifyAndBuildSignedMessage_InvalidMessage()
      throws IOException, GeneralSecurityException {
    ProxyKey proxyKey = sampleProxyKey();
    thrown.expect(IllegalArgumentException.class);

    MessageVerificationService verificationService = mock(MessageVerificationService.class);
    doThrow(new IllegalArgumentException("Invalid Message")).when(verificationService)
        .verify(any(SignedMessage.class));

    MessageSigningService service = MessageSigningService.builder()
        .serializer(messageSerializerService)
        .cryptographyService(cryptographyService)
        .signatureAlgorithm("SHA256WithRSAEncryption")
        .build();
    SimpleSignableMessage signableMessage = new SimpleSignableMessage();
    SignedMessage<SimpleSignableMessage> signedMessage = service.sign(signableMessage, proxyKey);

    MessageFactory messageFactory = MessageFactory.builder()
        .serializer(messageSerializerService)
        .cryptographyService(cryptographyService)
        .verificationService(verificationService)
        .build();

    messageFactory.verifyAndPopulateSignedMessage(signedMessage);
  }

  @ToString
  static class SimpleSignableMessage implements SignableMessage {

    public String message = "hello";

    @Override
    public ProxyId signer() {
      return ProxyId.of("dummy", "SHA256");
    }

    @Override
    public String toReadableString() {
      return "dummy";
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
      return true;
    }
  }

  @ToString
  static class ComplexSignableMessage implements SignableMessage {

    public SignedMessage<SimpleSignableMessage> internalObject;

    @Override
    public ProxyId signer() {
      return ProxyId.of("dummy", "SHA256");
    }

    @Override
    public String toReadableString() {
      return "dummy";
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
      return true;
    }
  }


}
