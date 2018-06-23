package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.SignableMessage;
import lombok.SneakyThrows;
import lombok.val;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.*;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class MessageSigningServiceTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private CryptographyService cryptographyService = BcCryptographyService.builder().pemService(PemService.builder().build()).build();

    @SneakyThrows
    private PrivateKey samplePrivateKey() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair().getPrivate();
    }

    @Test
    public void testSign() throws IOException, GeneralSecurityException {
        Proxy proxy = Proxy.builder().id("dummy").privateKey(samplePrivateKey()).sha256Thumbprint("SHA256").certificate(mock(Certificate.class)).build();

        SignableMessage signableMessage = new SignableMessage() {
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
