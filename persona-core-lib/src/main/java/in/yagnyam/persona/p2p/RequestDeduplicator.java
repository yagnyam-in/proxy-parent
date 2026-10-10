package in.yagnyam.persona.p2p;

import in.yagnyam.persona.PersonaId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Server-side de-duplication of {@code RequestMessage.requestId} (protocol
 * §5.2: "recipients MUST reject duplicate request ids from the same
 * persona"; §11 rule 5; §14 item 5) with the same "first wins, duplicates
 * rejected" semantics as the registrar's request-id uniqueness (protocol
 * §7.1).
 *
 * <p><strong>Memory bound.</strong> A dedup store that grows forever is a
 * denial-of-service vector (any peer can mint unbounded request ids), so
 * this store is <em>bounded</em>: it keeps at most {@code capacity}
 * entries in a synchronized access-order LRU map and evicts the
 * least-recently-used entry past the bound.
 *
 * <p><strong>Eviction semantics.</strong> Eviction makes replay protection
 * best-effort over a sliding window rather than absolute: a request id
 * re-presented <em>after</em> its entry was evicted is accepted and
 * processed again, never wrongly rejected as a duplicate. That is the safe
 * failure direction for exactly-once-looking protocols whose handlers are
 * idempotent (which is why §5.2 wants dedup in the first place); the
 * reverse — wrongly rejecting a fresh request — would break legitimate
 * flows. Deployments with hard exactly-once requirements must persist the
 * dedup set alongside the processed-request journal, which is out of scope
 * for this library (see {@code GAPS.md}).
 *
 * <p>Keying: duplicates are per <em>persona</em> (protocol §5.2), so the
 * key is the sender's bare persona id together with the request id — the
 * same request id from two different personas is two different requests.
 *
 * <p>Thread-safety: all methods are safe for concurrent use; the
 * check-and-record of one key is atomic, so under concurrent duplicates of
 * the same {@code (sender, requestId)} exactly one caller is accepted.
 */
public final class RequestDeduplicator {

  /** Default capacity: 10,000 request ids per peer namespace. */
  public static final int DEFAULT_CAPACITY = 10_000;

  private final int capacity;
  private final Map<String, Boolean> seen;

  /**
   * Creates a deduplicator with {@link #DEFAULT_CAPACITY}.
   */
  public RequestDeduplicator() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates a deduplicator holding at most {@code capacity} request ids.
   *
   * @param capacity maximum number of remembered {@code (sender, requestId)}
   *                 pairs; must be at least 1
   * @throws IllegalArgumentException if {@code capacity} is less than 1
   */
  public RequestDeduplicator(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be at least 1, found " + capacity);
    }
    this.capacity = capacity;
    this.seen = new LinkedHashMap<>(16, 0.75f, true) {
      private static final long serialVersionUID = 1L;

      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
        return size() > capacity;
      }
    };
  }

  /**
   * Checks a request id against the store and records it when new
   * (protocol §5.2).
   *
   * @param sender    the persona the request claims to come from — already
   *                  verified by the envelope layer before this check is
   *                  meaningful (an unverified sender id is attacker-chosen)
   * @param requestId the payload's {@code requestId()} (protocol §5.2)
   * @return {@code true} if this is the first occurrence of the pair and
   *     the request should be processed; {@code false} if it is a duplicate
   *     and MUST be rejected (protocol §11 rule 5)
   */
  public boolean checkAndRecord(PersonaId sender, String requestId) {
    Objects.requireNonNull(sender, "sender");
    Objects.requireNonNull(requestId, "requestId");
    // NUL separates the two free-form strings: persona ids are "any
    // URL-safe string" (protocol §3.2) and request ids are
    // application-chosen, so a printable separator could be spoofed.
    String key = sender.id() + '\u0000' + requestId;
    synchronized (seen) {
      if (seen.containsKey(key)) {
        // Touch for LRU: a live conversation's ids must not age out.
        seen.get(key);
        return false;
      }
      seen.put(key, Boolean.TRUE);
      return true;
    }
  }

  /**
   * @return the number of remembered {@code (sender, requestId)} pairs
   *     (at most the capacity)
   */
  public int size() {
    synchronized (seen) {
      return seen.size();
    }
  }

  /**
   * @return the maximum number of remembered pairs
   */
  public int capacity() {
    return capacity;
  }

  @Override
  public String toString() {
    return "RequestDeduplicator[size=" + size() + ", capacity=" + capacity + "]";
  }
}
