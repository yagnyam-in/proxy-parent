package in.yagnyam.persona.message;

/**
 * A payload that participates in an idempotent request/response flow
 * (protocol §5.2).
 *
 * <p>The payload carries a unique {@link #requestId()}; recipients MUST
 * reject duplicate request ids from the same persona (protocol §5.2, §11
 * rule 5). De-duplication is a recipient-side service (protocol §13
 * {@code RequestDedup}); this contract only requires the identifier to be
 * present.
 *
 * <p>Orthogonal to {@link SignableMessage}: a request message is signed like
 * any other payload.
 */
public interface RequestMessage {

  /**
   * The request identifier, globally unique within the recipient's namespace
   * (protocol §5.2).
   *
   * @return request id, never {@code null} for a valid payload
   */
  String requestId();
}
