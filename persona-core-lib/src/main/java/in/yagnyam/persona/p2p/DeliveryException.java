package in.yagnyam.persona.p2p;

/**
 * A transport could not deliver an envelope (protocol §13: the protocol
 * itself defines no delivery guarantees, so a transport that cannot deliver
 * fails loudly rather than dropping silently).
 *
 * <p>Delivery failures are operational failures of the transport, never
 * verification outcomes — verification results are values
 * ({@link in.yagnyam.persona.envelope.VerificationResult}).
 */
public class DeliveryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * @param message why the delivery failed
   */
  public DeliveryException(String message) {
    super(message);
  }

  /**
   * @param message why the delivery failed
   * @param cause   underlying transport failure
   */
  public DeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
