package in.yagnyam.persona.p2p;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A single-process {@link PersonaTransport} for tests and the runnable
 * demo: persona addresses resolve to listeners registered in the same JVM.
 *
 * <p>Although everything happens in one process, this transport faithfully
 * models a hostile network boundary (protocol §4, §12):
 * <ul>
 *   <li>receivers get <em>bytes only</em> — a defensive copy of the bytes
 *       handed to {@link #send}, never an object reference — so all
 *       verification happens on decoded bytes, exactly as on a real
 *       network;</li>
 *   <li>mutating the sender's array after {@link #send} returns has no
 *       effect on what the receiver sees (the transport copies at the
 *       boundary);</li>
 *   <li>an address with no registered listener is unreachable and fails
 *       loudly with a {@link DeliveryException} — a transport must not
 *       silently drop (protocol §13 defines no delivery guarantees, so the
 *       honest failure is the caller's signal).</li>
 * </ul>
 *
 * <p>Thread-safety: listener registration and delivery are backed by a
 * {@link ConcurrentHashMap}; listeners are invoked synchronously on the
 * sending thread (one listener per address — a second {@link #listen} for
 * the same id is rejected). {@link #send} blocks only as long as the
 * receiver's listener runs; listeners must not block.
 */
public final class InMemoryTransport implements PersonaTransport {

  private final ConcurrentMap<String, Listener> listeners = new ConcurrentHashMap<>();

  /**
   * Delivers a copy of {@code envelopeBytes} to the listener registered for
   * {@code recipientId}.
   *
   * @param recipientId   bare persona id of the recipient
   * @param envelopeBytes encoded envelope bytes (copied before delivery)
   * @throws DeliveryException if no listener is registered for
   *     {@code recipientId}
   */
  @Override
  public void send(String recipientId, byte[] envelopeBytes) {
    Objects.requireNonNull(recipientId, "recipientId");
    Objects.requireNonNull(envelopeBytes, "envelopeBytes");
    Listener listener = listeners.get(recipientId);
    if (listener == null) {
      throw new DeliveryException(
          "no receiver registered for persona '" + recipientId + "' on this transport");
    }
    listener.onEnvelope(envelopeBytes.clone());
  }

  /**
   * Registers {@code listener} as the sole receiver for {@code personaId}.
   *
   * @param personaId bare persona id to receive for
   * @param listener  receiver callback
   * @return a handle whose {@code close()} unregisters the listener
   * @throws IllegalStateException if a listener is already registered for
   *     {@code personaId}
   */
  @Override
  public AutoCloseable listen(String personaId, Listener listener) {
    Objects.requireNonNull(personaId, "personaId");
    Objects.requireNonNull(listener, "listener");
    if (listeners.putIfAbsent(personaId, listener) != null) {
      throw new IllegalStateException(
          "a listener is already registered for persona '" + personaId + "'");
    }
    return () -> listeners.remove(personaId, listener);
  }

  /**
   * @param personaId bare persona id
   * @return {@code true} if a listener is currently registered for
   *     {@code personaId}
   */
  public boolean hasReceiver(String personaId) {
    Objects.requireNonNull(personaId, "personaId");
    return listeners.containsKey(personaId);
  }

  @Override
  public String toString() {
    return "InMemoryTransport[receivers=" + listeners.keySet() + "]";
  }
}
