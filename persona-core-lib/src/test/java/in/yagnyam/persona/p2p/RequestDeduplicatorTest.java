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
 * two-tier model. Tier A (identity uniqueness) records
 * {@code (recipient, requestId, bareSignerId)} per verified signer and
 * rejects any verified delivery whose signer set overlaps a recorded one
 * — including a retry from the <em>same requester</em> with a
 * <em>different payload</em> under a reused requestId. Tier B (byte-level
 * replay) records {@code (recipient, preimageDigest)} and rejects any
 * redelivery of the same authenticated bytes regardless of how the
 * signature set was respelled, reordered, grown or split. Both tiers are
 * bounded flat LRU stores with safe-direction eviction (a re-request
 * after eviction is processed again, never wrongly rejected).
 *
 * <p>The preimage itself is envelope data, so the respelling cases are
 * exercised end-to-end in {@code PersonaMessengerTest}; here digests
 * stand in for them (one digest = one authenticated payload).
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

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-1")))
        .isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-1")))
        .isFalse();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-1")))
        .isFalse();
    assertThat(dedup.checkAndRecord(BOB, "req-2", Set.of("alice"), digest("payload-2")))
        .isTrue();
    // 2 tier-A signer records + 2 tier-B digest records.
    assertThat(dedup.size()).isEqualTo(4);
  }

  @Test
  void tierBRejectsEveryRespellingOfOneCapturedEnvelope() {
    // Reorder, pinned/unpinned respelling, an incremental co-signing
    // superset and a disjoint-quorum split of one captured envelope all
    // carry the SAME preimage bytes (the signatures array is not part of
    // the §6.4 preimage), hence the same digest: every one of them is a
    // replay of the first delivery at tier B — even a variant whose
    // signer set is DISJOINT from the recorded one, which tier A alone
    // could not catch.
    RequestDeduplicator dedup = new RequestDeduplicator();
    String captured = digest("captured-envelope-preimage");

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("a", "b"), captured)).isTrue();
    // Same digest, overlapping signers (reorder/respell/superset).
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("b", "a"), captured)).isFalse();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("a", "b", "c"), captured)).isFalse();
    // Same digest, DISJOINT signers (disjoint-quorum split): tier B.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("c", "d"), captured)).isFalse();
  }

  @Test
  void tierARejectsASameRequesterRetryWithADifferentPayload() {
    // §5.2/§11 rule 5: requestId uniqueness is per requesting Persona,
    // not per byte sequence. A second verified request from an
    // OVERLAPPING signer set under the same requestId is a duplicate even
    // though its payload — and hence its preimage digest — differs.
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-A")))
        .isTrue();
    // Same requester, same requestId, different payload: tier A rejects.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-B")))
        .isFalse();
    // An overlapping multi-signer set is likewise a duplicate.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice", "carol"),
        digest("payload-C"))).isFalse();
    // ...and each delivery still dedups against itself at tier B.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-A")))
        .isFalse();
  }

  @Test
  void aDisjointSignerSetReusingARequestIdWithADifferentPayloadIsADistinctRequest() {
    // §5.2 scopes uniqueness to "the same Persona": a joint request
    // authorized by a DISJOINT quorum is a different authorized operation
    // and is processed by design (neither tier matches: no signer
    // overlap, distinct digest).
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("a", "b"), digest("payload-A")))
        .isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("c", "d"), digest("payload-B")))
        .isTrue();
    // Each processed request then dedups against itself.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("a", "b"), digest("payload-A")))
        .isFalse();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("c", "d"), digest("payload-B")))
        .isFalse();
  }

  @Test
  void pinnedAndUnpinnedSpellingsShareTheTierANamespace() {
    // Signer ids are recorded as BARE persona ids (protocol §6.2: pinned
    // and unpinned spellings resolving to one certificate are one
    // signer), so a respelled retry from the same persona is a duplicate.
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-A")))
        .isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), digest("payload-B")))
        .isFalse();
  }

  @Test
  void duplicatesAreScopedPerRecipientPersona() {
    RequestDeduplicator dedup = new RequestDeduplicator();
    String payload = digest("payload-1");

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), payload)).isTrue();
    // The same request delivered to a different recipient is processed by
    // each recipient; only a replay to the SAME recipient is rejected.
    assertThat(dedup.checkAndRecord(ALICE, "req-1", Set.of("alice"), payload)).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("alice"), payload)).isFalse();
    assertThat(dedup.checkAndRecord(ALICE, "req-1", Set.of("alice"), payload)).isFalse();
  }

  @Test
  void evictionIsBoundedAndARerequestAfterEvictionIsProcessedAgain() {
    // Capacity 2 per tier; distinct signers so tier A admits each request.
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s1"), digest("payload-1"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-2", Set.of("s2"), digest("payload-2"))).isTrue();
    // Capacity exceeded: req-1's records (least recently used) are evicted.
    assertThat(dedup.checkAndRecord(BOB, "req-3", Set.of("s3"), digest("payload-3"))).isTrue();
    assertThat(dedup.size()).isEqualTo(4);

    // The evicted record is accepted again — eviction must never turn
    // into a wrong rejection of a re-request.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s1"), digest("payload-1"))).isTrue();
    // ...and it is once again deduplicated.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s1"), digest("payload-1"))).isFalse();
  }

  @Test
  void lruKeepsLiveEntriesOverColdOnes() {
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    dedup.checkAndRecord(BOB, "hot", Set.of("s1"), digest("hot"));
    dedup.checkAndRecord(BOB, "cold", Set.of("s2"), digest("cold"));
    // Touch "hot": "cold" becomes the eviction candidate.
    assertThat(dedup.checkAndRecord(BOB, "hot", Set.of("s1"), digest("hot"))).isFalse();

    dedup.checkAndRecord(BOB, "new", Set.of("s3"), digest("new"));

    assertThat(dedup.checkAndRecord(BOB, "hot", Set.of("s1"), digest("hot"))).isFalse();
    assertThat(dedup.checkAndRecord(BOB, "cold", Set.of("s2"), digest("cold"))).isTrue();
  }

  @Test
  void theCapacityBoundsRecordsInBothTiersWithinOneRecipientRequestIdBucket() {
    // Finding: no per-key list may grow without limit. With flat records,
    // N distinct payloads (tier B) and N distinct signers (tier A) under
    // one (recipient, requestId) are N ordinary records in each tier and
    // the capacity bounds them: oldest first evicted, re-delivery of an
    // evicted record reprocessed (the safe direction — reprocessing,
    // never a wrong rejection).
    RequestDeduplicator dedup = new RequestDeduplicator(3);

    // Tier B growth: three distinct payloads from DISJOINT signers
    // (so tier A admits them) under one (recipient, requestId).
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s1"), digest("p1"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s2"), digest("p2"))).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s3"), digest("p3"))).isTrue();
    // A fourth exceeds the capacity in both tiers: s1/p1 evicted.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s4"), digest("p4"))).isTrue();
    assertThat(dedup.size()).isEqualTo(6);

    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s2"), digest("p2"))).isFalse();
    // s1/p1 were evicted from both tiers: re-delivery is reprocessed.
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s1"), digest("p1"))).isTrue();
    assertThat(dedup.size()).isEqualTo(6);
    assertThat(dedup.checkAndRecord(BOB, "req-1", Set.of("s1"), digest("p1"))).isFalse();

    // Tier A growth: multi-signer requests record one entry per signer,
    // still bounded by the same flat capacity — a 4-signer request under
    // capacity 3 evicts rather than growing a per-bucket list.
    RequestDeduplicator small = new RequestDeduplicator(3);
    assertThat(small.checkAndRecord(BOB, "joint", Set.of("a", "b", "c", "d"),
        digest("joint-payload"))).isTrue();
    assertThat(small.size()).isLessThanOrEqualTo(2 * 3);
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
          if (dedup.checkAndRecord(BOB, "same-request", Set.of("alice"),
              digest("same-payload"))) {
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
  void underConcurrentSameRequesterRetriesWithDifferentPayloadsExactlyOneIsAccepted()
      throws InterruptedException {
    // Tier A under contention: 32 threads race the SAME requestId from
    // the SAME signer, each with a DIFFERENT payload (different digest).
    // Byte-level keys alone would accept all 32; identity uniqueness
    // accepts exactly one — the first verified delivery wins.
    RequestDeduplicator dedup = new RequestDeduplicator();
    int threads = 32;
    AtomicInteger accepted = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int t = 0; t < threads; t++) {
      int thread = t;
      pool.submit(() -> {
        try {
          start.await();
          if (dedup.checkAndRecord(BOB, "same-request", Set.of("alice"),
              digest("payload-" + thread))) {
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
            if (!dedup.checkAndRecord(BOB, requestId, Set.of("s" + thread),
                digest(requestId))) {
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
    assertThat(dedup.size()).isEqualTo(2 * threads * requestsPerThread);
  }

  @Test
  void concurrentMixedDuplicatesAndFreshRequestsAcceptEachDistinctRequestExactlyOnce()
      throws InterruptedException {
    // 32 threads hammering a capacity-64-per-tier store with a mix of
    // duplicate and fresh requests: every distinct request is accepted
    // exactly once while its records are live, the bound holds, and
    // nothing deadlocks. Fresh requests stay well under the capacity so
    // no live record is evicted mid-test (eviction would legitimately
    // re-accept).
    RequestDeduplicator dedup = new RequestDeduplicator(64);
    int distinct = 16;
    int threads = 32;
    ConcurrentHashMap<String, AtomicInteger> acceptedPerRequest = new ConcurrentHashMap<>();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int t = 0; t < threads; t++) {
      int thread = t;
      pool.submit(() -> {
        try {
          start.await();
          for (int r = 0; r < 20; r++) {
            int slot = r % distinct;
            String requestId = "req-" + slot;
            if (dedup.checkAndRecord(BOB, requestId, Set.of("signer-" + slot),
                digest("payload-" + slot))) {
              acceptedPerRequest.computeIfAbsent(requestId, k -> new AtomicInteger())
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

    assertThat(acceptedPerRequest).hasSize(distinct);
    acceptedPerRequest.forEach((requestId, count) ->
        assertThat(count).as("acceptances of %s", requestId).hasValue(1));
    assertThat(dedup.size()).isEqualTo(2 * distinct);
  }

  @Test
  void invalidConstructionAndArgumentsAreRejected() {
    assertThatThrownBy(() -> new RequestDeduplicator(0))
        .isInstanceOf(IllegalArgumentException.class);
    RequestDeduplicator dedup = new RequestDeduplicator(1);
    assertThatThrownBy(() -> dedup.checkAndRecord(null, "req", Set.of("s"), digest("p")))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, null, Set.of("s"), digest("p")))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, "req", null, digest("p")))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, "req", Set.of("s"), null))
        .isInstanceOf(NullPointerException.class);
    // A verified envelope always has at least one verified signature.
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, "req", Set.of(), digest("p")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
