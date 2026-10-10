package in.yagnyam.persona.p2p;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
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

  @Test
  void firstOccurrenceIsAcceptedAndDuplicatesAreRejected() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isFalse();
    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isFalse();
    assertThat(dedup.checkAndRecord(ALICE, "req-2")).isTrue();
    assertThat(dedup.size()).isEqualTo(2);
  }

  @Test
  void duplicatesAreScopedPerPersona() {
    RequestDeduplicator dedup = new RequestDeduplicator();

    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isTrue();
    // The same requestId from a different persona is a different request.
    assertThat(dedup.checkAndRecord(BOB, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(BOB, "req-1")).isFalse();
  }

  @Test
  void pinnedAndUnpinnedSpellingsOfOnePersonaShareTheNamespace() {
    RequestDeduplicator dedup = new RequestDeduplicator();
    PersonaId pinned = PersonaId.of("alice#"
        + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    assertThat(dedup.checkAndRecord(pinned, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isFalse();
  }

  @Test
  void evictionIsBoundedAndARerequestAfterEvictionIsProcessedAgain() {
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isTrue();
    assertThat(dedup.checkAndRecord(ALICE, "req-2")).isTrue();
    // Capacity exceeded: req-1 (least recently used) is evicted.
    assertThat(dedup.checkAndRecord(ALICE, "req-3")).isTrue();
    assertThat(dedup.size()).isEqualTo(2);

    // The evicted id is accepted again — eviction must never turn into a
    // wrong rejection of a re-request.
    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isTrue();
    // ...and it is once again deduplicated.
    assertThat(dedup.checkAndRecord(ALICE, "req-1")).isFalse();
  }

  @Test
  void lruKeepsLiveEntriesOverColdOnes() {
    RequestDeduplicator dedup = new RequestDeduplicator(2);

    dedup.checkAndRecord(ALICE, "hot");
    dedup.checkAndRecord(ALICE, "cold");
    // Touch "hot": "cold" becomes the eviction candidate.
    assertThat(dedup.checkAndRecord(ALICE, "hot")).isFalse();

    dedup.checkAndRecord(ALICE, "new");

    assertThat(dedup.checkAndRecord(ALICE, "hot")).isFalse();
    assertThat(dedup.checkAndRecord(ALICE, "cold")).isTrue();
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
          if (dedup.checkAndRecord(ALICE, "same-request")) {
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
            if (!dedup.checkAndRecord(ALICE, requestId)) {
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
  void invalidConstructionAndArgumentsAreRejected() {
    assertThatThrownBy(() -> new RequestDeduplicator(0))
        .isInstanceOf(IllegalArgumentException.class);
    RequestDeduplicator dedup = new RequestDeduplicator(1);
    assertThatThrownBy(() -> dedup.checkAndRecord(null, "req"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dedup.checkAndRecord(ALICE, null))
        .isInstanceOf(NullPointerException.class);
  }
}
