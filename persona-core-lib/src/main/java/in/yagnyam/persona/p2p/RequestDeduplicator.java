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
 * <p><strong>Keying: the authenticated payload, not the signature
 * set.</strong> A delivered request is recorded under its recipient, its
 * {@code requestId} and the SHA-256 digest of the envelope's §6.4 signing
 * preimage ({@code domain ‖ messageType ‖ payloadFormat ‖ schemaId ‖
 * payload}) — exactly the bytes every signature of the envelope
 * authenticates. Two deliveries are duplicates iff they carry the
 * <em>same authenticated bytes</em> to the same recipient, independently
 * of which subset of the original signatures they retain:
 *
 * <ul>
 *   <li><strong>Wire respellings of one captured envelope dedup.</strong>
 *   Signature order is unauthenticated (protocol §7.4: "Ordering of
 *   signatures MUST NOT affect verification outcome") and pinned vs
 *   unpinned {@code signedBy} spellings are unauthenticated too, but
 *   neither touches the preimage: a reordered or respelled redelivery
 *   digests identically and is a replay.</li>
 *   <li><strong>Incremental §7.4 co-signing dedups.</strong> Each added
 *   co-signature yields a new envelope that <em>supersedes</em> the
 *   previous one (protocol §5.5) with the same requestId and identical
 *   payload bytes — only the authorization grew. The preimage is
 *   unchanged, so the stronger envelope is a replay: the safe direction
 *   (no double execution of one request).</li>
 *   <li><strong>Disjoint-quorum splits of one captured envelope dedup.
 *   </strong> A transport attacker can split a captured envelope with,
 *   say, 4 signatures and quorum 2 into two deliveries retaining disjoint
 *   subsets — every retained signature still verifies and each subset
 *   meets quorum, but both deliveries authenticate the <em>same preimage
 *   bytes</em>, so the second is a replay. (This is the case a
 *   signer-set-overlap keying could not catch; the preimage digest
 *   subsumes signer-set logic entirely because the preimage is identical
 *   across reorder, respell, superset <em>and</em> disjoint subsets of
 *   one captured envelope.)</li>
 *   <li><strong>A genuinely different payload is a distinct request</strong>
 *   even when it reuses a {@code requestId}: different payload bytes
 *   digest differently and are processed. Reusing a request id across
 *   different payloads is a sender bug the protocol forbids (§5.2:
 *   request ids are "globally unique within the recipient's namespace");
 *   dedup must never wrongly reject such a delivery.</li>
 * </ul>
 *
 * <p>Only <em>verified</em> envelopes may reach this store (verification
 * runs first on the receive path): an unauthenticated digest is
 * attacker-chosen and would otherwise poison the store to DoS a real
 * requester.
 *
 * <p><strong>Memory bound.</strong> A dedup store that grows forever is a
 * denial-of-service vector (any peer can mint unbounded request ids and
 * unbounded payload variants), so the store is <em>bounded</em>: it keeps
 * at most {@code capacity} records — each record one
 * {@code (recipient, requestId, preimage digest)} triple — in a
 * synchronized access-order LRU map, evicting the least-recently-used
 * record past the bound. The bound applies to <em>records</em>, not just
 * to distinct request ids: because the digest is part of the key, any
 * number of distinct payloads submitted under one
 * {@code (recipient, requestId)} pair still occupy at most
 * {@code capacity} records in total and evict one another oldest-first.
 *
 * <p><strong>Eviction semantics.</strong> Eviction makes replay protection
 * best-effort over a sliding window rather than absolute: a request
 * re-presented <em>after</em> its record was evicted is accepted and
 * processed again, never wrongly rejected as a duplicate. That is the safe
 * failure direction for exactly-once-looking protocols whose handlers are
 * idempotent (which is why §5.2 wants dedup in the first place); the
 * reverse — wrongly rejecting a fresh request — would break legitimate
 * flows. Deployments with hard exactly-once requirements must persist the
 * dedup set alongside the processed-request journal, which is out of scope
 * for this library (see {@code GAPS.md}).
 *
 * <p>Thread-safety: all methods are safe for concurrent use; the
 * check-and-record of one request is atomic, so under concurrent duplicate
 * deliveries of the same authenticated request exactly one caller is
 * accepted.
 */
public final class RequestDeduplicator {

  /** Default capacity: 10,000 remembered authenticated requests. */
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
   * Creates a deduplicator holding at most {@code capacity} records.
   *
   * @param capacity maximum number of remembered
   *                 {@code (recipient, requestId, preimage digest)}
   *                 records; must be at least 1
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
   * Checks a request against the store and records it when new (protocol
   * §5.2). The duplicate predicate is equality of the full
   * {@code (recipient, requestId, preimageDigest)} triple — see the class
   * javadoc for why the authenticated payload digest (not the signer set,
   * which is unauthenticated in order, spelling and subset) is the dedup
   * identity.
   *
   * @param recipient      the persona the request was delivered to (the
   *                       dedup scope: a replay to the same recipient is
   *                       rejected, while the same request reaching a
   *                       different recipient is a distinct delivery)
   * @param requestId      the payload's {@code requestId()} (protocol
   *                       §5.2)
   * @param preimageDigest SHA-256 digest (lowercase hex) of the envelope's
   *                       §6.4 signing preimage — the exact bytes every
   *                       signature of the delivered envelope
   *                       authenticated; meaningful only after the
   *                       envelope verified (an unverified digest is
   *                       attacker-chosen and must never reach this
   *                       store, or a forged request would poison it and
   *                       DoS the real requester)
   * @return {@code true} if this is the first delivery of these
   *     authenticated bytes to this recipient and the request should be
   *     processed; {@code false} if it is a duplicate and MUST be
   *     rejected (protocol §11 rule 5)
   */
  public boolean checkAndRecord(PersonaId recipient, String requestId, String preimageDigest) {
    Objects.requireNonNull(recipient, "recipient");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(preimageDigest, "preimageDigest");
    // NUL separates the free-form strings: persona ids are "any
    // URL-safe string" (protocol §3.2) and request ids are
    // application-chosen, so a printable separator could be spoofed.
    String key = recipient.id() + '\u0000' + requestId + '\u0000' + preimageDigest;
    synchronized (seen) {
      if (seen.containsKey(key)) {
        seen.get(key); // touch for LRU: a replayed request stays hot
        return false;
      }
      seen.put(key, Boolean.TRUE);
      return true;
    }
  }

  /**
   * @return the number of remembered
   *     {@code (recipient, requestId, preimage digest)} records (at most
   *     the capacity)
   */
  public int size() {
    synchronized (seen) {
      return seen.size();
    }
  }

  /**
   * @return the maximum number of remembered records
   */
  public int capacity() {
    return capacity;
  }

  @Override
  public String toString() {
    return "RequestDeduplicator[size=" + size() + ", capacity=" + capacity + "]";
  }
}
