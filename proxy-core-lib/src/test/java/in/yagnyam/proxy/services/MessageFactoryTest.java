package in.yagnyam.proxy.services;

import in.yagnyam.proxy.*;
import lombok.SneakyThrows;
import lombok.ToString;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class MessageFactoryTest {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private MessageSerializerService messageSerializerService = new MessageSerializerService();

    @ToString
    static class SimpleSignableMessage implements SignableMessage {
        public String message = "hello";
        @Override
        public String signer() {
            return "dummy";
        }
    }


    @ToString
    static class ComplexSignableMessage implements SignableMessage {
        public SignedMessage<SimpleSignableMessage> internalObject;
        @Override
        public String signer() {
            return "dummy";
        }
    }

    @SneakyThrows
    PrivateKey samplePrivateKey() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair().getPrivate();
    }

    Proxy sampleProxy() {
        return Proxy.builder().id("dummy").privateKey(samplePrivateKey()).sha256Thumbprint("SHA256").certificate(mock(Certificate.class)).build();
    }


    @Test
    public void testVerifyAndBuildSignedMessage() throws IOException {
        Proxy proxy = sampleProxy();
        MessageSignerService service = MessageSignerService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(CryptographyService.instance())
                .signatureAlgorithm("SHA256WithRSAEncryption")
                .build();
        SimpleSignableMessage signableMessage = new SimpleSignableMessage();
        SignedMessage<SimpleSignableMessage> signedMessage = service.sign(signableMessage, proxy);

        MessageFactory messageFactory = MessageFactory.builder()
                .serializer(messageSerializerService)
                .cryptographyService(CryptographyService.instance())
                .build();

        messageFactory.verifySignedMessage(signedMessage);
    }


    @Test
    public void testVerifyAndBuildSignedMessage_InnerObject() throws IOException {
        Proxy proxy = sampleProxy();
        MessageSignerService service = MessageSignerService.builder()
                .serializer(messageSerializerService)
                .cryptographyService(CryptographyService.instance())
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
                .cryptographyService(CryptographyService.instance())
                .build();

        SignedMessage<ComplexSignableMessage> result = messageFactory.buildSignedMessage(signedText);
        System.out.println(result);
        System.out.println(result.getMessage().internalObject);
        assertNotNull(result.getMessage());
    }



}
