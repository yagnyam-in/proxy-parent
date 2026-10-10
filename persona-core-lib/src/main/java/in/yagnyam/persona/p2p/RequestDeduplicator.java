package in.yagnyam.persona.p2p;

import in.yagnyam.persona.PersonaId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Server-side de-duplication of {@code RequestMessage.requestId} (protocol
 * §5.2: "recipients MUST reject duplicate request ids from the same
 * persona"; §11 rule 5; §14 item 5) with the same "first wins, duplicates
 * rejected" semantics as the registrar's request-id uniqueness (protocol
 * §7.1).
 *
 * <p><strong>Two tiers; a delivery must pass BOTH.</strong> Neither tier
 * alone is sufficient, so each verified request is checked against two
 * bounded stores, and rejected when either tier matches:
 *
 * <ul>
 *   <li><strong>Tier A — identity uniqueness (§5.2, §11 rule 5).</strong>
 *   Records one {@code (recipient, requestId, signerId)} entry per
 *   verified signer of each processed request, keyed on <em>bare</em>
 *   persona ids so pinned and unpinned spellings of one persona share
 *   the namespace. A verified delivery is a duplicate when its signer
 *   set <em>overlaps</em> any signer recorded for the same
 *   {@code (recipient, requestId)} pair. This is what §5.2 actually
 *   requires: uniqueness is scoped to "the same Persona", so a second
 *   request from an overlapping requester — even one carrying a
 *   <em>different payload</em> under a reused requestId — is a replay
 *   and is rejected. Conversely a <em>disjoint</em> signer set reusing
 *   a requestId with a different payload passes tier A by design: a
 *   joint request authorized by a disjoint quorum is a different
 *   authorized operation, not a replay of the first.</li>
 *   <li><strong>Tier B — byte-level replay.</strong> Records one
 *   {@code (recipient, preimageDigest)} entry per processed request,
 *   where the digest is SHA-256 of the envelope's §6.4 signing preimage
 *   ({@code domain ‖ messageType ‖ payloadFormat ‖ schemaId ‖ payload})
 *   — exactly the bytes every signature of the envelope authenticates.
 *   A verified delivery is a duplicate when its authenticated bytes were
 *   already processed for this recipient, <em>regardless of which
 *   signature subset, ordering or spelling carried them</em>. The
 *   requestId is not part of the key because it is already part of the
 *   authenticated payload bytes, hence of the digest: the digest subsumes
 *   it, and adding it would only split one replay class across two keys.
 *   </li>
 * </ul>
 *
 * <p>Which tier catches which attack:
 *
 * <ul>
 *   <li><strong>Reorder / respell / incremental-superset of one captured
 *   envelope → tier B</strong> (tier A catches them too). Signature order
 *   is unauthenticated (protocol §7.4: "Ordering of signatures MUST NOT
 *   affect verification outcome"), pinned vs unpinned {@code signedBy}
 *   spellings are unauthenticated, and an added §7.4 co-signature yields
 *   a superseding envelope (§5.5) with identical payload bytes — none of
 *   these touch the preimage, so every such redelivery digests
 *   identically and tier B rejects it.</li>
 *   <li><strong>Disjoint-quorum split of one captured envelope → tier B
 *   alone.</strong> A transport attacker can split a captured envelope
 *   with, say, 4 signatures and quorum 2 into two deliveries retaining
 *   disjoint subsets {a,b} and {c,d} — every retained signature verifies
 *   and each subset meets quorum, and the signer sets are disjoint so
 *   tier A lets the second delivery through. But both deliveries
 *   authenticate the <em>same preimage bytes</em>, so tier B rejects the
 *   second as a replay.</li>
 *   <li><strong>Same requester, same requestId, new payload → tier A
 *   alone.</strong> A retry with a changed payload (or a different
 *   payloadFormat encoding of it) digests differently and sails through
 *   tier B, but its signer set overlaps the recorded one, so tier A
 *   rejects it — §5.2 uniqueness is per requester, not per byte
 *   sequence.</li>
 *   <li><strong>Disjoint quorum, same requestId, new payload → processed
 *   by design.</strong> Neither tier matches (no signer overlap, distinct
 *   digest): this is a different authorized operation that happens to
 *   reuse a requestId, which §5.2 permits because uniqueness is scoped
 *   to the requesting Persona.</li>
 * </ul>
 *
 * <p>Only <em>verified</em> envelopes may reach this store (verification
 * runs first on the receive path): an unauthenticated digest or signer
 * id is attacker-chosen and would otherwise poison the store to DoS a
 * real requester.
 *
 * <p><strong>Memory bound.</strong> A dedup store that grows forever is a
 * denial-of-service vector (any peer can mint unbounded request ids and
 * unbounded payload variants), so <em>both tiers</em> are bounded: each
 * keeps at most {@code capacity} flat records in a synchronized
 * access-order LRU map, evicting the least-recently-used record past the
 * bound. There are no per-key lists: N distinct signers under one
 * {@code (recipient, requestId)} pair are N ordinary tier-A records and
 * N distinct payloads under one pair are N ordinary tier-B records, all
 * sharing the same capacity budget and evicting one another
 * oldest-first.
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
 * check-and-record of one request across both tiers is atomic, so under
 * concurrent duplicate deliveries of the same authenticated request
 * exactly one caller is accepted.
 */
public final class RequestDeduplicator {

  /** Default capacity: 10,000 remembered records per tier. */
  public static final int DEFAULT_CAPACITY = 10_000;

  private final int capacity;
  /** Tier A: {@code (recipient, requestId, signerId)} per verified signer. */
  private final Map<String, Boolean> signerRecords;
  /** Tier B: {@code (recipient, preimageDigest)} per processed request. */
  private final Map<String, Boolean> digestRecords;
  /** One lock for both tiers: the two-tier check-and-record is atomic. */
  private final Object lock = new Object();

  /**
   * Creates a deduplicator with {@link #DEFAULT_CAPACITY}.
   */
  public RequestDeduplicator() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates a deduplicator holding at most {@code capacity} records
   * <em>per tier</em>.
   *
   * @param capacity maximum number of remembered records in each tier
   *                 ({@code (recipient, requestId, signerId)} entries in
   *                 tier A and {@code (recipient, preimageDigest)}
   *                 entries in tier B); must be at least 1
   * @throws IllegalArgumentException if {@code capacity} is less than 1
   */
  public RequestDeduplicator(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be at least 1, found " + capacity);
    }
    this.capacity = capacity;
    this.signerRecords = boundedLru(capacity);
    this.digestRecords = boundedLru(capacity);
  }

  /**
   * Checks a request against both tiers and records it when new (protocol
   * §5.2). The duplicate predicate is: the signer set overlaps a recorded
   * signer of the same {@code (recipient, requestId)} pair (tier A,
   * identity uniqueness), <em>or</em> the exact authenticated bytes were
   * already processed for this recipient (tier B, byte-level replay) —
   * see the class javadoc for which tier catches which attack.
   *
   * @param recipient      the persona the request was delivered to (the
   *                       dedup scope: a replay to the same recipient is
   *                       rejected, while the same request reaching a
   *                       different recipient is a distinct delivery)
   * @param requestId      the payload's {@code requestId()} (protocol
   *                       §5.2)
   * @param signerIds      the bare persona ids of the envelope's verified
   *                       signers (protocol §6.2: pinned and unpinned
   *                       spellings resolving to one certificate are one
   *                       signer, so the bare id is the identity that
   *                       counts); must be non-empty — a verified
   *                       envelope has at least one verified signature
   * @param preimageDigest SHA-256 digest (lowercase hex) of the envelope's
   *                       §6.4 signing preimage — the exact bytes every
   *                       signature of the delivered envelope
   *                       authenticated; meaningful only after the
   *                       envelope verified (an unverified digest is
   *                       attacker-chosen and must never reach this
   *                       store, or a forged request would poison it and
   *                       DoS the real requester)
   * @return {@code true} if this is the first verified delivery of the
   *     request by these signers with these authenticated bytes and the
   *     request should be processed; {@code false} if it is a duplicate
   *     and MUST be rejected (protocol §11 rule 5)
   */
  public boolean checkAndRecord(
      PersonaId recipient, String requestId, Set<String> signerIds, String preimageDigest) {
    Objects.requireNonNull(recipient, "recipient");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(signerIds, "signerIds");
    Objects.requireNonNull(preimageDigest, "preimageDigest");
    if (signerIds.isEmpty()) {
      throw new IllegalArgumentException("a verified request has at least one signer");
    }
    // NUL separates the free-form strings: persona ids are "any
    // URL-safe string" (protocol §3.2) and request ids are
    // application-chosen, so a printable separator could be spoofed.
    String scope = recipient.id() + '\u0000' + requestId + '\u0000';
    List<String> signerKeys = new ArrayList<>(signerIds.size());
    for (String signerId : signerIds) {
      signerKeys.add(scope + Objects.requireNonNull(signerId, "signer id"));
    }
    String digestKey = recipient.id() + '\u0000' + preimageDigest;
    synchronized (lock) {
      boolean duplicate = false;
      for (String signerKey : signerKeys) {
        // Touch each hit for LRU: a replayed request stays hot.
        duplicate |= signerRecords.get(signerKey) != null;
      }
      duplicate |= digestRecords.get(digestKey) != null;
      if (duplicate) {
        return false;
      }
      for (String signerKey : signerKeys) {
        signerRecords.put(signerKey, Boolean.TRUE);
      }
      digestRecords.put(digestKey, Boolean.TRUE);
      return true;
    }
  }

  /**
   * @return the number of remembered records across both tiers (at most
   *     twice the capacity)
   */
  public int size() {
    synchronized (lock) {
      return signerRecords.size() + digestRecords.size();
    }
  }

  /**
   * @return the maximum number of remembered records per tier
   */
  public int capacity() {
    return capacity;
  }

  @Override
  public String toString() {
    synchronized (lock) {
      return "RequestDeduplicator[signerRecords=" + signerRecords.size()
          + ", digestRecords=" + digestRecords.size() + ", capacity=" + capacity + "]";
    }
  }

  /** A synchronized access-order LRU map bounded to {@code capacity}. */
  private static Map<String, Boolean> boundedLru(int capacity) {
    return new LinkedHashMap<>(16, 0.75f, true) {
      private static final long serialVersionUID = 1L;

      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
        return size() > capacity;
      }
    };
  }
}
