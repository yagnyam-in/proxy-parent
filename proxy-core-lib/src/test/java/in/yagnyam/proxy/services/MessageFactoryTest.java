package in.yagnyam.proxy.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.Proxy;
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

    private CryptographyService cryptographyService = BcCryptographyService.builder().pemService(PemService.builder().build()).build();

    @ToString
    static class SimpleSignableMessage implements SignableMessage {
        public String message = "hello";

        @Override
        public String signer() {
            return "dummy";
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
        public String signer() {
            return "dummy";
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

    @SneakyThrows
    private PrivateKey samplePrivateKey() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair().getPrivate();
    }

    private Proxy sampleProxy() {
        return Proxy.builder().id("dummy").privateKey(samplePrivateKey()).sha256Thumbprint("SHA256").certificate(mock(Certificate.class)).build();
    }


    @Test
    public void testVerifyAndBuildSignedMessage() throws IOException, GeneralSecurityException {
        Proxy proxy = sampleProxy();

        MessageVerificationService verificationService = mock(MessageVerificationService.class);
        doAnswer(invocation -> null).when(verificationService).verify(any(SignedMessage.class));

        MessageSigningService service = MessageSigningService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .signatureAlgorithm("SHA256WithRSAEncryption")
                .build();
        SimpleSignableMessage signableMessage = new SimpleSignableMessage();
        SignedMessage<SimpleSignableMessage> signedMessage = service.sign(signableMessage, proxy);

        MessageFactory messageFactory = MessageFactory.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .verificationService(verificationService)
                .build();

        messageFactory.verifySignedMessage(signedMessage);
    }


    @Test
    public void testVerifyAndBuildSignedMessage_InnerObject() throws IOException, GeneralSecurityException {
        Proxy proxy = sampleProxy();

        MessageVerificationService verificationService = mock(MessageVerificationService.class);
        doAnswer(invocation -> null).when(verificationService).verify(any(SignedMessage.class));

        MessageSigningService service = MessageSigningService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .signatureAlgorithm("SHA256WithRSAEncryption")
                .build();

        SimpleSignableMessage simpleMessage = new SimpleSignableMessage();
        SignedMessage<SimpleSignableMessage> signedSimpleMessage = service.sign(simpleMessage, proxy);

        ComplexSignableMessage complexMessage = new ComplexSignableMessage();
        complexMessage.internalObject = signedSimpleMessage;
        SignedMessage<ComplexSignableMessage> signedComplexMessage = service.sign(complexMessage, proxy);
        String signedText = messageSerializerService.serialize(signedComplexMessage);

        MessageFactory messageFactory = MessageFactory.builder()
                .serializer(messageSerializerService)
                .verificationService(verificationService)
                .cryptographyService(cryptographyService)
                .build();

        SignedMessage<ComplexSignableMessage> result = messageFactory.buildSignedMessage(signedText);
        System.out.println(result);
        System.out.println(result.getMessage().internalObject);
        assertNotNull(result.getMessage());
    }


    @Test
    public void testVerifyAndBuildSignedMessage_InvalidMessage() throws IOException, GeneralSecurityException {
        Proxy proxy = sampleProxy();
        thrown.expect(IllegalArgumentException.class);

        MessageVerificationService verificationService = mock(MessageVerificationService.class);
        doThrow(new IllegalArgumentException("Invalid Message")).when(verificationService).verify(any(SignedMessage.class));

        MessageSigningService service = MessageSigningService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .signatureAlgorithm("SHA256WithRSAEncryption")
                .build();
        SimpleSignableMessage signableMessage = new SimpleSignableMessage();
        SignedMessage<SimpleSignableMessage> signedMessage = service.sign(signableMessage, proxy);

        MessageFactory messageFactory = MessageFactory.builder()
                .serializer(messageSerializerService)
                .cryptographyService(cryptographyService)
                .verificationService(verificationService)
                .build();

        messageFactory.verifySignedMessage(signedMessage);
    }


}
