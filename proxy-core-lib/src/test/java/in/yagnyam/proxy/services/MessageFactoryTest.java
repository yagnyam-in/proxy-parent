package in.yagnyam.proxy.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.ProxyKey;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.SneakyThrows;
import lombok.ToString;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.*;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class MessageFactoryTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private MessageSerializerService messageSerializerService = new MessageSerializerService();

    private MessageBuilder messageBuilder = MessageBuilder.builder().serializer(messageSerializerService).build();

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
    public void testVerifySignedMessage() throws IOException, GeneralSecurityException {
        ProxyKey proxyKey = sampleProxyKey();

        MessageVerificationService verificationService = mock(MessageVerificationService.class);
        doAnswer(invocation -> null).when(verificationService).verify(any(SignedMessage.class));

        MessageSigningService service = MessageSigningService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .build();
        SimpleSignableMessage signableMessage = new SimpleSignableMessage();
        SignedMessage<SimpleSignableMessage> signedMessage = service.singleSign(signableMessage, proxyKey);

        MessageFactory messageFactory = MessageFactory.builder()
                .messageBuilder(messageBuilder)
                .verificationService(verificationService)
                .build();

        messageFactory.verifySignedMessage(signedMessage);
    }

    @Test
    public void testBuildAndVerifySignedMessageVerifyAndBuildSignedMessage_InnerObject()
            throws IOException, GeneralSecurityException {
        ProxyKey proxyKey = sampleProxyKey();

        MessageVerificationService verificationService = mock(MessageVerificationService.class);
        doAnswer(invocation -> null).when(verificationService).verify(any(SignedMessage.class));

        MessageSigningService service = MessageSigningService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .build();

        SimpleSignableMessage simpleMessage = new SimpleSignableMessage();
        SignedMessage<SimpleSignableMessage> signedSimpleMessage = service
                .singleSign(simpleMessage, proxyKey);

        ComplexSignableMessage complexMessage = new ComplexSignableMessage();
        complexMessage.internalObject = signedSimpleMessage;
        SignedMessage<ComplexSignableMessage> signedComplexMessage = service
                .singleSign(complexMessage, proxyKey);
        String signedText = messageSerializerService.serializeSignedMessage(signedComplexMessage);
        System.out.println("signedText: " + signedText);

        MessageFactory messageFactory = MessageFactory.builder()
                .messageBuilder(messageBuilder)
                .verificationService(verificationService)
                .build();

        SignedMessage<ComplexSignableMessage> result = messageFactory
                .buildAndVerifySignedMessage(signedText, ComplexSignableMessage.class);
        System.out.println(result);
        System.out.println(result.getMessage().internalObject);
        assertNotNull(result.getMessage());
    }

    @Test
    public void testVerifySignedMessage_InvalidMessage()
            throws IOException, GeneralSecurityException {
        ProxyKey proxyKey = sampleProxyKey();
        thrown.expect(IllegalArgumentException.class);

        MessageVerificationService verificationService = mock(MessageVerificationService.class);
        doThrow(new IllegalArgumentException("Invalid Message")).when(verificationService)
                .verify(any(SignedMessage.class));

        MessageSigningService service = MessageSigningService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .build();
        SimpleSignableMessage signableMessage = new SimpleSignableMessage();
        SignedMessage<SimpleSignableMessage> signedMessage = service.singleSign(signableMessage, proxyKey);

        MessageFactory messageFactory = MessageFactory.builder()
                .messageBuilder(messageBuilder)
                .verificationService(verificationService)
                .build();

        messageFactory.verifySignedMessage(signedMessage);
    }

    @ToString
    static class SimpleSignableMessage implements SignableMessage {

        public String message = "hello";

        @Override
        public ProxyId signer() {
            return ProxyId.of("dummy", "SHA256");
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
        @JsonIgnore
        public boolean isValid() {
            return true;
        }
    }


}
