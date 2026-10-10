package in.yagnyam.persona.p2p;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.crypto.Digests;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RequestDeduplicator} (protocol §5.2, §11 rule 5): the
 * dedup identity is the recipient-scoped digest of the §6.4 signing
 * preimage — the authenticated bytes — so every wire respelling of one
 * captured envelope (reordered, re-spelled, co-signing superset, or a
 * disjoint-quorum split of the signature set) digests identically and
 * dedups, while a genuinely different payload is a distinct request. The
 * store is bounded per <em>record</em> (not per request id) with LRU
 * eviction, and eviction is the safe direction (a re-request after
 * eviction is processed again, never wrongly rejected).
 *
 * <p>The preimage itself is envelope data, so the respelling cases are
 * exercised end-to-end in {@code PersonaMessengerTest}; here digests stand
 * in for them (one digest = one authenticated payload).
 */
class RequestDeduplicatorTest {

  private static final PersonaId ALICE = PersonaId.of("alice");
  private static final PersonaId BOB = PersonaId.of("bob");

  /** A stand-in preimage digest: SHA-256 of the label, hex — opaque here. */
  private static String digest(String label) {
    return Digests.sha256Hex(label.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @Test
  void firstOccurrenceIsAcceptedAndDuplicatesAreRejected() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-1"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-1"))).isFalse();
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-1"))).isFalse();
    assertThat(dedup.checkAndRecord(BOB, "req-2", digest("payload-2"))).isTrue();
    assertThat(dedup.size()).isEqualTo(2);
  }

  @Test
  void theDigestIsTheDedupIdentityRegardlessOfHowTheEnvelopeWasRespelled() {
    // Reorder, pinned/unpinned respelling, an incremental co-signing
    // superset and a disjoint-quorum split of one captured envelope all
    // carry the SAME preimage bytes (the signatures array is not part of
    // the §6.4 preimage), hence the same digest: every one of them is a
    // replay of the first delivery. One digest stands in for all of them.
    RequestDeduplicator dedup = new RequestDeduplicator();
    String captured = digest("captured-envelope-preimage");

    assertThat(dedup.checkAndRecord(BOB, "req-1", captured)).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", captured)).isFalse();
  }

  @Test
  void aDifferentPayloadReusingARequestIdIsADistinctRequest() {
    // Different payload bytes digest differently: both are processed.
    // Reusing a requestId across payloads is a sender bug §5.2 forbids;
    // dedup must never wrongly reject the second delivery.
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-A"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-B"))).isTrue();
    // ...and each distinct payload dedups against itself.
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-A"))).isFalse();
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-B"))).isFalse();
  }

  @Test
  void duplicatesAreScopedPerRecipientPersona() {
    RequestDeduplicator dedup = new RequestDeduplicator();
    String payload = digest("payload-1");

    assertThat(dedup.checkAndRecord(BOB, "req-1", payload)).isTrue();
    // The same request delivered to a different recipient is processed by
    // each recipient; only a replay to the SAME recipient is rejected.
    assertThat(dedup.checkAndRecord(ALICE, "req-1", payload)).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", payload)).isFalse();
    assertThat(dedup.checkAndRecord(ALICE, "req-1", payload)).isFalse();
  }

  @Test
  void evictionIsBoundedAndARerequestAfterEvictionIsProcessedAgain() {
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-1"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-2", digest("payload-2"))).isTrue();
    // Capacity exceeded: req-1 (least recently used) is evicted.
    assertThat(dedup.checkAndRecord(BOB, "req-3", digest("payload-3"))).isTrue();
    assertThat(dedup.size()).isEqualTo(2);

    // The evicted record is accepted again — eviction must never turn
    // into a wrong rejection of a re-request.
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-1"))).isTrue();
    // ...and it is once again deduplicated.
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("payload-1"))).isFalse();
  }

  @Test
  void lruKeepsLiveEntriesOverColdOnes() {
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    dedup.checkAndRecord(BOB, "hot", digest("hot"));
    dedup.checkAndRecord(BOB, "cold", digest("cold"));
    // Touch "hot": "cold" becomes the eviction candidate.
    assertThat(dedup.checkAndRecord(BOB, "hot", digest("hot"))).isFalse();

    dedup.checkAndRecord(BOB, "new", digest("new"));

    assertThat(dedup.checkAndRecord(BOB, "hot", digest("hot"))).isFalse();
    assertThat(dedup.checkAndRecord(BOB, "cold", digest("cold"))).isTrue();
  }

  @Test
  void theCapacityBoundsRecordsWithinOneRecipientRequestIdBucket() {
    // Finding: a per-bucket list of records must not grow without limit.
    // With the digest in the key, N distinct payloads under one
    // (recipient, requestId) are N ordinary records and the capacity
    // bounds them: oldest first evicted, re-delivery of an evicted record
    // reprocessed (the safe direction — reprocessing, never a wrong
    // rejection).
    RequestDeduplicator dedup = new RequestDeduplicator(3);

    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("p1"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("p2"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("p3"))).isTrue();
    // A fourth distinct payload under the SAME (recipient, requestId)
    // bucket exceeds the capacity: the bound holds and p1 is evicted.
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("p4"))).isTrue();
    assertThat(dedup.size()).isEqualTo(3);

    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("p2"))).isFalse();
    // p1 was evicted: its re-delivery is reprocessed, not rejected.
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("p1"))).isTrue();
    assertThat(dedup.size()).isEqualTo(3);
    assertThat(dedup.checkAndRecord(BOB, "req-1", digest("p1"))).isFalse();
  }

  @Test
  void underConcurrentDuplicatesExactlyOneCallerIsAccepted() throws InterruptedException {
    RequestDeduplicator dedup = new RequestDeduplicator();
    int threads = 32;
    AtomicInteger accepted = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int t = 0; t < threads; t++) {
      pool.submit(() -> {
        try {
          start.await();
          if (dedup.checkAndRecord(BOB, "same-request", digest("same-payload"))) {
            accepted.incrementAndGet();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    assertThat(accepted.get()).isEqualTo(1);
  }

  @Test
  void concurrentDistinctRequestsAreAllAccepted() throws InterruptedException {
    RequestDeduplicator dedup = new RequestDeduplicator();
    int threads = 16;
    int requestsPerThread = 50;
    Set<String> rejected = ConcurrentHashMap.newKeySet();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int t = 0; t < threads; t++) {
      int thread = t;
      pool.submit(() -> {
        try {
          start.await();
          for (int i = 0; i < requestsPerThread; i++) {
            String requestId = "t" + thread + "-" + i;
            if (!dedup.checkAndRecord(BOB, requestId, digest(requestId))) {
              rejected.add(requestId);
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    assertThat(rejected).isEmpty();
    assertThat(dedup.size()).isEqualTo(threads * requestsPerThread);
  }

  @Test
  void concurrentMixedDuplicatesAndFreshPayloadsAcceptEachDistinctPreimageExactlyOnce()
      throws InterruptedException {
    // 32 threads hammering a capacity-64 store with a mix of duplicate
    // and fresh (requestId, payload) pairs: every distinct preimage is
    // accepted exactly once while its record is live, the bound holds,
    // and nothing deadlocks. Fresh pairs stay well under the capacity so
    // no live record is evicted mid-test (eviction would legitimately
    // re-accept).
    RequestDeduplicator dedup = new RequestDeduplicator(64);
    int distinct = 16;
    int threads = 32;
    ConcurrentHashMap<String, AtomicInteger> acceptedPerDigest = new ConcurrentHashMap<>();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int t = 0; t < threads; t++) {
      int thread = t;
      pool.submit(() -> {
        try {
          start.await();
          for (int r = 0; r < 20; r++) {
            String requestId = "req-" + (r % distinct);
            String payload = digest("payload-" + (r % distinct));
            if (dedup.checkAndRecord(BOB, requestId, payload)) {
              acceptedPerDigest.computeIfAbsent(requestId, k -> new AtomicInteger())
                  .incrementAndGet();
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    assertThat(acceptedPerDigest).hasSize(distinct);
    acceptedPerDigest.forEach((requestId, count) ->
        assertThat(count).as("acceptances of %s", requestId).hasValue(1));
    assertThat(dedup.size()).isEqualTo(distinct);
  }

  @Test
  void invalidConstructionAndArgumentsAreRejected() {
    assertThatThrownBy(() -> new RequestDeduplicator(0))
        .isInstanceOf(IllegalArgumentException.class);
    RequestDeduplicator dedup = new RequestDeduplicator(1);
    assertThatThrownBy(() -> dedup.checkAndRecord(null, "req", digest("p")))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, null, digest("p")))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, "req", null))
        .isInstanceOf(NullPointerException.class);
  }
}
