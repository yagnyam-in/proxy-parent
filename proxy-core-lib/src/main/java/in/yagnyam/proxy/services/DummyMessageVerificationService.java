package in.yagnyam.proxy.services;

import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(staticName = "instance")
public class DummyMessageVerificationService extends MessageVerificationService {

  @Override
  public <T extends SignableMessage> void verify(@NonNull SignedMessage<T> message) {
    log.warn("Using DummyMessageVerificationService.");
  }
}