package in.yagnyam.proxy.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.SignedMessageSignature;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import lombok.ToString;
import lombok.val;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageVerificationServiceTest {

  static class DummySignableMessage implements SignableMessage {

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

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindSignature_NoSignature() {
    SignedMessage<DummySignableMessage> signedMessage = SignedMessage.<DummySignableMessage>builder()
        .type(DummySignableMessage.class.getName())
        .payload("Dummy")
        .signature(SignedMessageSignature.of("def", "dummy"))
        .signedBy(ProxyId.of("dummy"))
        .build();
    MessageVerificationService.findSignature(signedMessage, "abc");
  }

  @Test
  public void testFindSignature() {
    DummySignableMessage signableMessage = new DummySignableMessage();
    SignedMessage<DummySignableMessage> signedMessage = SignedMessage.<DummySignableMessage>builder()
        .message(signableMessage)
        .signedBy(signableMessage.signer())
        .type(DummySignableMessage.class.getName())
        .payload("Dummy")
        .signature(SignedMessageSignature.of("abc", "dummy"))
        .build();
    val signature = MessageVerificationService.findSignature(signedMessage, "abc");
    assertEquals("dummy", signature.getValue());
  }

  @Test(expected = IllegalStateException.class)
  public void testGetSignerProxy_NotExtracted() {
    ProxyResolver proxyResolver = mock(ProxyResolver.class);
    SignedMessage<DummySignableMessage> signedMessage = SignedMessage.<DummySignableMessage>builder()
        .type(DummySignableMessage.class.getName())
        .payload("Dummy")
        .signature(SignedMessageSignature.of("abc", "dummy"))
        .signedBy(ProxyId.of("dummy"))
        .build();
    verificationService(proxyResolver).getSignerProxy(signedMessage);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testGetSignerProxy_InvalidProxy() {
    ProxyResolver proxyResolver = mock(ProxyResolver.class);
    when(proxyResolver.resolveProxy(any())).thenReturn(Collections.emptyList());
    DummySignableMessage signableMessage = new DummySignableMessage();
    SignedMessage<DummySignableMessage> signedMessage = SignedMessage.<DummySignableMessage>builder()
        .type(DummySignableMessage.class.getName())
        .message(signableMessage)
        .payload("Dummy")
        .signedBy(signableMessage.signer())
        .signature(SignedMessageSignature.of("abc", "dummy"))
        .build();
    verificationService(proxyResolver).getSignerProxy(signedMessage);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testGetSignerProxy_AmbiguousProxy() {
    ProxyResolver proxyResolver = mock(ProxyResolver.class);
    when(proxyResolver.resolveProxy(any())).thenReturn(Arrays.asList(mock(Proxy.class), mock(Proxy.class)));
    DummySignableMessage signableMessage = new DummySignableMessage();
    SignedMessage<DummySignableMessage> signedMessage = SignedMessage.<DummySignableMessage>builder()
        .type(DummySignableMessage.class.getName())
        .message(signableMessage)
        .signedBy(signableMessage.signer())
        .payload("Dummy")
        .signature(SignedMessageSignature.of("abc", "dummy"))
        .build();
    verificationService(proxyResolver).getSignerProxy(signedMessage);
  }


  @Test
  public void testGetSignerProxy_UniqueProxy() {
    ProxyResolver proxyResolver = mock(ProxyResolver.class);
    when(proxyResolver.resolveProxy(any())).thenReturn(Collections.singletonList(mock(Proxy.class)));
    DummySignableMessage signableMessage = new DummySignableMessage();
    SignedMessage<DummySignableMessage> signedMessage = SignedMessage.<DummySignableMessage>builder()
        .type(DummySignableMessage.class.getName())
        .message(signableMessage)
        .signedBy(signableMessage.signer())
        .payload("Dummy")
        .signature(SignedMessageSignature.of("abc", "dummy"))
        .build();
    assertNotNull(verificationService(proxyResolver).getSignerProxy(signedMessage));
  }



  private MessageVerificationService verificationService(ProxyResolver proxyResolver) {
    return MessageVerificationService.builder()
        .cryptographyService(mock(CryptographyService.class))
        .proxyResolver(proxyResolver)
        .signatureAlgorithm("dummy")
        .build();
  }

}
