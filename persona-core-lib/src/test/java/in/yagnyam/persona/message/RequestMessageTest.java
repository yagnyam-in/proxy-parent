package in.yagnyam.persona.message;

import static org.assertj.core.api.Assertions.assertThat;

import in.yagnyam.persona.PersonaId;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the orthogonal {@link RequestMessage} (protocol §5.2) and
 * {@link AddressableMessage} (protocol §5.3) contracts: they compose with
 * {@link SignableMessage} on one payload without any of the contracts
 * bleeding into the others.
 */
class RequestMessageTest {

  /**
   * A request-response payload that is also addressable and signable: the
   * three orthogonal contracts of §5.1-§5.3 on one record.
   */
  private record TestPaymentOrder(
      String paymentOrderId,
      PersonaId requester,
      PersonaId recipient) implements SignableMessage, RequestMessage, AddressableMessage {

    @Override
    public String messageType() {
      return "example.PaymentOrder@v1";
    }

    @Override
    public PersonaId signer() {
      return requester;
    }

    @Override
    public String requestId() {
      return paymentOrderId;
    }

    @Override
    public PersonaId address() {
      return recipient;
    }

    @Override
    public boolean isValid() {
      return paymentOrderId != null && !paymentOrderId.isEmpty()
          && requester != null && requester.isValid()
          && recipient != null && recipient.isValid();
    }
  }

  @Test
  void theThreeContractsComposeOnOnePayload() {
    PersonaId alice = PersonaId.of("p-alice");
    PersonaId bob = PersonaId.of("p-bob");
    TestPaymentOrder order = new TestPaymentOrder("order-42", alice, bob);

    // §5.1
    assertThat(order.messageType()).isEqualTo("example.PaymentOrder@v1");
    assertThat(order.signer()).isEqualTo(alice);
    assertThat(order.validSigners()).isEqualTo(Set.of(alice));
    assertThat(order.canBeSignedBy(alice)).isTrue();
    assertThat(order.canBeSignedBy(bob)).isFalse();
    // §5.2
    assertThat(order.requestId()).isEqualTo("order-42");
    // §5.3
    assertThat(order.address()).isEqualTo(bob);
    assertThat(order.isValid()).isTrue();
  }

  @Test
  void theContractsStayIndependent() {
    PersonaId alice = PersonaId.of("p-alice");
    TestPaymentOrder order = new TestPaymentOrder("", alice, alice);

    // The §5.1 self-check (isValid) can fail while §5.2/§5.3 members stay
    // readable: nothing in one contract constrains another
    assertThat(order.requestId()).isEmpty();
    assertThat(order.address()).isEqualTo(alice);
    assertThat(order.isValid()).isFalse();
  }
}
