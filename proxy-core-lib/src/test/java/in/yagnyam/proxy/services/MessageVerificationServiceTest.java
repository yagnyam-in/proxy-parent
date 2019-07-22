package in.yagnyam.proxy.services;

import static org.mockito.Mockito.mock;

import in.yagnyam.proxy.MultiSignableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import java.security.Security;
import java.util.Set;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageVerificationServiceTest {

  static class DummySingleSignableMessage implements SignableMessage {

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
  }


  static class DummyMultiSignableMessage implements MultiSignableMessage {

    @Override
    public boolean validateSigners(Set<ProxyId> signers) {
      return false;
    }

    @Override
    public boolean hasRequiredSignatures(Set<ProxyId> signers) {
      return false;
    }

    @Override
    public String toReadableString() {
      return "dummy";
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean canBeSignedBy(ProxyId signerId) {
      return false;
    }
  }


  static {
    Security.addProvider(new BouncyCastleProvider());
  }


  private MessageVerificationService verificationService(ProxyResolver proxyResolver) {
    return MessageVerificationService.builder()
        .cryptographyService(mock(CryptographyService.class))
        .proxyResolver(proxyResolver)
        .build();
  }


  @Test
  public void testVerify_InvalidMessage() {

  }

  @Test
  public void testIsValid() {
  }

  @Test
  public void testIsComplete() {

  }

}
