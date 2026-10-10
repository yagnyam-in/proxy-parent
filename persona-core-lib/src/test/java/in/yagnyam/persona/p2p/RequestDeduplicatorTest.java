package in.yagnyam.persona.p2p;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RequestDeduplicator} (protocol §5.2, §11 rule 5): first
 * wins, duplicates rejected, per-persona scoping, bounded LRU eviction —
 * and that eviction is the safe direction (a re-request after eviction is
 * processed again, never wrongly rejected).
 */
class RequestDeduplicatorTest {

  private static final PersonaId ALICE = PersonaId.of("alice");
  private static final PersonaId BOB = PersonaId.of("bob");
  private static final PersonaId CAROL = PersonaId.of("carol");
  private static final PersonaId DAVE = PersonaId.of("dave");

  @Test
  void firstOccurrenceIsAcceptedAndDuplicatesAreRejected() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isFalse();
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isFalse();
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-2")).isTrue();
    assertThat(dedup.size()).isEqualTo(2);
  }

  @Test
  void duplicatesAreScopedPerSenderPersona() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isTrue();
    // The same requestId from a different sender is a different request.
    assertThat(dedup.checkAndRecord(BOB, BOB, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(BOB, BOB, "req-1")).isFalse();
  }

  @Test
  void duplicatesAreScopedPerRecipientPersona() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isTrue();
    // The same request delivered to a different recipient is processed by
    // each recipient; only a replay to the SAME recipient is rejected.
    assertThat(dedup.checkAndRecord(ALICE, ALICE, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isFalse();
    assertThat(dedup.checkAndRecord(ALICE, ALICE, "req-1")).isFalse();
  }

  @Test
  void pinnedAndUnpinnedSpellingsOfOnePersonaShareTheNamespace() {
    RequestDeduplicator dedup = new RequestDeduplicator();
    PersonaId pinned = PersonaId.of("alice#"
        + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    assertThat(dedup.checkAndRecord(BOB, pinned, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isFalse();
  }

  @Test
  void evictionIsBoundedAndARerequestAfterEvictionIsProcessedAgain() {
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-2")).isTrue();
    // Capacity exceeded: req-1 (least recently used) is evicted.
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-3")).isTrue();
    assertThat(dedup.size()).isEqualTo(2);

    // The evicted id is accepted again — eviction must never turn into a
    // wrong rejection of a re-request.
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isTrue();
    // ...and it is once again deduplicated.
    assertThat(dedup.checkAndRecord(BOB, ALICE, "req-1")).isFalse();
  }

  @Test
  void lruKeepsLiveEntriesOverColdOnes() {
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    dedup.checkAndRecord(BOB, ALICE, "hot");
    dedup.checkAndRecord(BOB, ALICE, "cold");
    // Touch "hot": "cold" becomes the eviction candidate.
    assertThat(dedup.checkAndRecord(BOB, ALICE, "hot")).isFalse();

    dedup.checkAndRecord(BOB, ALICE, "new");

    assertThat(dedup.checkAndRecord(BOB, ALICE, "hot")).isFalse();
    assertThat(dedup.checkAndRecord(BOB, ALICE, "cold")).isTrue();
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
          if (dedup.checkAndRecord(BOB, ALICE, "same-request")) {
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
            if (!dedup.checkAndRecord(BOB, ALICE, requestId)) {
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
  void multiSignerRequestsAreDeduplicatedOrderIndependently() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, List.of(ALICE, BOB), "req-1")).isTrue();
    // Signature order on the wire is unauthenticated (protocol §7.4): any
    // permutation of the same verified signer set is the same request.
    assertThat(dedup.checkAndRecord(BOB, List.of(BOB, ALICE), "req-1")).isFalse();
    assertThat(dedup.checkAndRecord(BOB, List.of(ALICE, BOB), "req-1")).isFalse();
    // Recipient scoping still applies to the set form.
    assertThat(dedup.checkAndRecord(ALICE, List.of(BOB, ALICE), "req-1")).isTrue();
  }

  @Test
  void overlappingSignerSetsOfOneRequestIdAreReplaysDisjointOnesAreNot() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(BOB, List.of(ALICE, BOB), "req-1")).isTrue();
    // Incremental §7.4 co-signing supersedes (§5.5): same requestId,
    // identical payload, stronger authorization — a replay, not a new
    // request (no double execution).
    assertThat(dedup.checkAndRecord(BOB, List.of(ALICE, BOB, CAROL), "req-1")).isFalse();
    // A subset and a partially overlapping set are replays too: the shared
    // signer already spent this requestId with this recipient.
    assertThat(dedup.checkAndRecord(BOB, List.of(ALICE), "req-1")).isFalse();
    assertThat(dedup.checkAndRecord(BOB, List.of(BOB, DAVE), "req-1")).isFalse();
    // A genuinely disjoint signer set is a different authorized request
    // (protocol §5.2: duplicates are per requester).
    assertThat(dedup.checkAndRecord(BOB, List.of(CAROL, DAVE), "req-1")).isTrue();
    // ...and the new set is recorded: its own permutations now dedup.
    assertThat(dedup.checkAndRecord(BOB, List.of(DAVE, CAROL), "req-1")).isFalse();
  }

  @Test
  void pinnedAndUnpinnedSpellingsShareTheMultiSignerNamespace() {
    RequestDeduplicator dedup = new RequestDeduplicator();
    PersonaId pinnedAlice = PersonaId.of("alice#"
        + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    assertThat(dedup.checkAndRecord(BOB, List.of(pinnedAlice, BOB), "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(BOB, List.of(BOB, ALICE), "req-1")).isFalse();
  }

  @Test
  void anEmptySignerSetIsRejected() {
    RequestDeduplicator dedup = new RequestDeduplicator();
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, List.of(), "req-1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, (List<PersonaId>) null, "req"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void invalidConstructionAndArgumentsAreRejected() {
    assertThatThrownBy(() -> new RequestDeduplicator(0))
        .isInstanceOf(IllegalArgumentException.class);
    RequestDeduplicator dedup = new RequestDeduplicator(1);
    assertThatThrownBy(() -> dedup.checkAndRecord(null, ALICE, "req"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, (PersonaId) null, "req"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(BOB, ALICE, null))
        .isInstanceOf(NullPointerException.class);
  }
}
