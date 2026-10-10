package in.yagnyam.persona.p2p;

import in.yagnyam.persona.PersonaId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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
 * <p><strong>Keying.</strong> Duplicates are per <em>recipient</em> and per
 * <em>requester</em> (protocol §5.2, §11 rule 5: "requestId MUST be unique
 * per (recipient, requester)"). The store key is the recipient's bare
 * persona id and the request id; recorded against each key are the
 * <em>verified signer sets</em> (sorted sets of bare persona ids) already
 * seen for it, and a delivery is a duplicate iff its signer set shares at
 * least one member with a recorded set. Recipient scoping is required
 * because the same request (same requester, same request id) legitimately
 * delivered to two different personas must be processed by each of them,
 * while a replay of that request to the <em>same</em> persona must be
 * rejected. Bare ids are used so pinned and unpinned spellings of one
 * persona share a namespace.
 *
 * <p><strong>Why signer-set overlap rather than one sender id.</strong> The
 * dedup identity of a request must be derived only from authenticated data
 * and must be independent of the signature order on the wire: protocol §7.4
 * requires verification to accept any order ("Ordering of signatures MUST
 * NOT affect verification outcome"), and the §6.4 preimage authenticates
 * the payload bytes, not the order of the {@code signatures} array. Keying
 * a multi-signed request on its first signer would let a transport attacker
 * capture an accepted envelope, swap the signature entries — a legal wire
 * transformation that still verifies — and resend it under a different
 * dedup key, delivering the same authenticated request to handlers twice.
 * The signer set is order-independent by construction, and every member is
 * authenticated: verification (which runs first) rejects the whole envelope
 * unless every entry verifies against an authorized, trusted signer.
 * Overlap (rather than set equality) is the duplicate predicate because the
 * request is one payload with one quorum: incremental §7.4 co-signing
 * yields a new envelope that <em>supersedes</em> the previous one (protocol
 * §5.5) with the same requestId and identical payload bytes — only the
 * authorization grew. Treating that stronger envelope as a replay is the
 * safe direction (no double execution of the same request), while a
 * <em>disjoint</em> signer set under the same request id is genuinely a
 * different authorized request (protocol §5.2: "duplicate request ids
 * <em>from the same persona</em>") and is accepted.
 *
 * <p>Thread-safety: all methods are safe for concurrent use; the
 * check-and-record of one request is atomic, so under concurrent duplicates
 * of the same {@code (recipient, requestId, signer set)} exactly one caller
 * is accepted.
 */
public final class RequestDeduplicator {

  /** Default capacity: 10,000 request ids per peer namespace. */
  public static final int DEFAULT_CAPACITY = 10_000;

  private final int capacity;
  private final Map<String, List<Set<String>>> seen;

  /**
   * Creates a deduplicator with {@link #DEFAULT_CAPACITY}.
   */
  public RequestDeduplicator() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates a deduplicator holding at most {@code capacity} request ids.
   *
   * @param capacity maximum number of remembered
   *                 {@code (recipient, requestId)} entries; must be
   *                 at least 1
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
      protected boolean removeEldestEntry(Map.Entry<String, List<Set<String>>> eldest) {
        return size() > capacity;
      }
    };
  }

  /**
   * Checks a request id against the store and records it when new
   * (protocol §5.2).
   *
   * @param recipient the persona the request was delivered to (the dedup
   *                  scope: a replay to the same recipient is rejected,
   *                  while the same request reaching a different recipient
   *                  is a distinct delivery)
   * @param sender    the persona the request claims to come from — already
   *                  verified by the envelope layer before this check is
   *                  meaningful (an unverified sender id is attacker-chosen
   *                  and must never reach this store, or a forged request id
   *                  would poison it and DoS the real requester)
   * @param requestId the payload's {@code requestId()} (protocol §5.2)
   * @return {@code true} if this is the first occurrence of the request and
   *     it should be processed; {@code false} if it is a duplicate
   *     and MUST be rejected (protocol §11 rule 5)
   */
  public boolean checkAndRecord(PersonaId recipient, PersonaId sender, String requestId) {
    Objects.requireNonNull(sender, "sender");
    return checkAndRecord(recipient, List.of(sender), requestId);
  }

  /**
   * Checks a request id against the store and records it when new, for a
   * request authenticated by a <em>set</em> of verified signers (protocol
   * §5.2 over a §7.4 multi-signed envelope). The duplicate predicate is
   * signer-set <em>overlap</em> under {@code (recipient, requestId)} — see
   * the class javadoc for the keying contract and why overlap (not equality
   * or wire order) is the rule.
   *
   * @param recipient the persona the request was delivered to (the dedup
   *                  scope)
   * @param senders   the envelope's verified signers — every signer of a
   *                  delivered envelope is verified (verification runs
   *                  first and rejects the whole envelope unless every
   *                  entry verifies), so the set is authenticated and its
   *                  order on the wire is irrelevant; must be non-empty
   * @param requestId the payload's {@code requestId()} (protocol §5.2)
   * @return {@code true} if no recorded signer set of this
   *     {@code (recipient, requestId)} shares a signer with
   *     {@code senders} (first occurrence — process the request);
   *     {@code false} on any overlap (duplicate — MUST be rejected,
   *     protocol §11 rule 5)
   * @throws IllegalArgumentException if {@code senders} is empty
   */
  public boolean checkAndRecord(
      PersonaId recipient, Collection<PersonaId> senders, String requestId) {
    Objects.requireNonNull(recipient, "recipient");
    Objects.requireNonNull(senders, "senders");
    Objects.requireNonNull(requestId, "requestId");
    // The dedup identity is a sorted set of BARE persona ids: order on the
    // wire is unauthenticated (protocol §7.4) and must not matter, and a
    // pinned 'alice#<thumbprint>' and unpinned 'alice' are one persona.
    Set<String> signerIds = new TreeSet<>();
    for (PersonaId sender : senders) {
      signerIds.add(Objects.requireNonNull(sender, "senders element").id());
    }
    if (signerIds.isEmpty()) {
      throw new IllegalArgumentException("senders must be non-empty: a delivered request"
          + " carries at least one verified signer");
    }
    // NUL separates the free-form strings: persona ids are "any
    // URL-safe string" (protocol §3.2) and request ids are
    // application-chosen, so a printable separator could be spoofed.
    String key = recipient.id() + '\u0000' + requestId;
    synchronized (seen) {
      List<Set<String>> recorded = seen.get(key); // a get also touches for LRU
      if (recorded != null) {
        for (Set<String> signerSet : recorded) {
          if (!Collections.disjoint(signerSet, signerIds)) {
            return false;
          }
        }
        // A genuinely different signer set under the same
        // (recipient, requestId): a different authorized request.
        recorded.add(signerIds);
        return true;
      }
      List<Set<String>> first = new ArrayList<>(1);
      first.add(signerIds);
      seen.put(key, first);
      return true;
    }
  }

  /**
   * @return the number of remembered {@code (recipient, requestId)}
   *     entries (at most the capacity)
   */
  public int size() {
    synchronized (seen) {
      return seen.size();
    }
  }

  /**
   * @return the maximum number of remembered entries
   */
  public int capacity() {
    return capacity;
  }

  @Override
  public String toString() {
    return "RequestDeduplicator[size=" + size() + ", capacity=" + capacity + "]";
  }
}
