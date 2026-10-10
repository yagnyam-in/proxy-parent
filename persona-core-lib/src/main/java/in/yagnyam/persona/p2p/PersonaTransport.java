package in.yagnyam.persona.p2p;

/**
 * The transport boundary of persona-to-persona messaging (protocol §13:
 * "over transport — HTTP, gRPC, MQTT, QR, mesh").
 *
 * <p>The protocol is transport-agnostic: it defines envelopes, not channels
 * (protocol §10, §12). A {@code PersonaTransport} is the minimal contract
 * every transport must provide for the language-level messenger: deliver an
 * <em>encoded envelope</em> (bytes) from one persona address to another.
 *
 * <p>Design choices, deliberately minimal:
 * <ul>
 *   <li><strong>Addressing is by bare persona id string</strong> (protocol
 *       §5.3 gives the addressed-message contract; the transport resolves
 *       how an id maps to an endpoint). The {@code #thumbprint} pin is an
 *       envelope-level concern (protocol §3.2) and plays no role in
 *       routing.</li>
 *   <li><strong>Push delivery</strong>: receivers register a {@link Listener}
 *       per persona id (§13 shows the envelope arriving at the peer's
 *       verifier). A pollable inbox would force every test and demo to
 *       thread-pump; a listener keeps the contract one method wide and
 *       models store-and-forward transports just as well (their poller is
 *       one {@code while} loop calling the same listener).</li>
 *   <li><strong>Bytes only at the boundary.</strong> What crosses
 *       {@link #send}/{@link Listener#onEnvelope} is the encoded envelope —
 *       never a Java object reference — so receivers always run the full
 *       verification of protocol §7.3 on decoded bytes, exactly as they
 *       must on a hostile network (protocol §4, §12).</li>
 *   <li><strong>No delivery guarantees.</strong> The protocol says nothing
 *       about reliability; a transport that cannot deliver MUST fail loudly
 *       ({@link DeliveryException}) rather than silently drop.</li>
 * </ul>
 *
 * <p>Implementations MUST be thread-safe: senders and listeners may be
 * concurrent. Listener callbacks are invoked with the transport's thread —
 * implementations document their threading; listeners must not block.
 *
 * <p>Confidentiality is NOT a transport responsibility this contract
 * implies: envelopes are signed, not encrypted (protocol §12); transports
 * that need channel security layer it underneath (TLS, WireGuard, ...).
 */
public interface PersonaTransport {

  /**
   * Delivers an encoded envelope to a persona address.
   *
   * @param recipientId    bare persona id of the recipient (the address
   *                       this transport resolves)
   * @param envelopeBytes  encoded envelope (transport encoding, e.g.
   *                       canonical JSON per {@code docs/SCHEMA.md} §5)
   * @throws NullPointerException if any argument is {@code null}
   * @throws DeliveryException if the recipient cannot be reached
   */
  void send(String recipientId, byte[] envelopeBytes);

  /**
   * Registers the receiver of envelopes addressed to {@code personaId}.
   *
   * @param personaId bare persona id to receive for
   * @param listener  callback receiving the encoded envelope bytes
   * @return a handle whose {@link AutoCloseable#close()} unregisters the
   *     listener
   * @throws NullPointerException if any argument is {@code null}
   * @throws IllegalStateException if a listener is already registered for
   *     {@code personaId}
   */
  AutoCloseable listen(String personaId, Listener listener);

  /**
   * Receiver callback. The bytes are the encoded envelope exactly as they
   * arrived; the listener owns no copy obligations (transports hand over
   * bytes the sender can no longer mutate).
   */
  @FunctionalInterface
  interface Listener {

    /**
     * Called with an encoded envelope addressed to the registered persona.
     *
     * @param envelopeBytes encoded envelope bytes
     */
    void onEnvelope(byte[] envelopeBytes);
  }
}
