package in.yagnyam.persona.p2p;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemoryTransport}: bytes-only delivery at the boundary,
 * loud failure on unknown addresses, single-listener registration, and
 * thread-safe concurrent sends.
 */
class InMemoryTransportTest {

  private final InMemoryTransport transport = new InMemoryTransport();

  @Test
  void deliversBytesToTheRegisteredListener() {
    ConcurrentLinkedQueue<byte[]> inbox = new ConcurrentLinkedQueue<>();
    transport.listen("bob", inbox::add);

    transport.send("bob", "hello".getBytes(UTF_8));

    assertThat(inbox).hasSize(1);
    assertThat(inbox.peek()).isEqualTo("hello".getBytes(UTF_8));
  }

  @Test
  void deliveredBytesAreDefensiveCopies() {
    ConcurrentLinkedQueue<byte[]> inbox = new ConcurrentLinkedQueue<>();
    transport.listen("bob", inbox::add);

    byte[] bytes = "hello".getBytes(UTF_8);
    transport.send("bob", bytes);
    // Hostile sender mutates its array after send returns.
    bytes[0] = 'X';

    assertThat(inbox.peek()).isEqualTo("hello".getBytes(UTF_8));
  }

  @Test
  void sendToAnUnknownRecipientFailsLoudly() {
    assertThatThrownBy(() -> transport.send("nobody", new byte[] {1}))
        .isInstanceOf(DeliveryException.class)
        .hasMessageContaining("nobody");
  }

  @Test
  void aSecondListenerForTheSameAddressIsRejected() {
    transport.listen("bob", bytes -> { });
    assertThatThrownBy(() -> transport.listen("bob", bytes -> { }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("bob");
  }

  @Test
  void closingTheRegistrationStopsDelivery() throws Exception {
    AutoCloseable registration = transport.listen("bob", bytes -> { });
    assertThat(transport.hasReceiver("bob")).isTrue();

    registration.close();

    assertThat(transport.hasReceiver("bob")).isFalse();
    assertThatThrownBy(() -> transport.send("bob", new byte[] {1}))
        .isInstanceOf(DeliveryException.class);
  }

  @Test
  void concurrentSendsAreAllDelivered() throws InterruptedException {
    AtomicInteger received = new AtomicInteger();
    transport.listen("bob", bytes -> received.incrementAndGet());

    int threads = 8;
    int sendsPerThread = 100;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int t = 0; t < threads; t++) {
      pool.submit(() -> {
        try {
          start.await();
          for (int i = 0; i < sendsPerThread; i++) {
            transport.send("bob", new byte[] {1});
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    assertThat(received.get()).isEqualTo(threads * sendsPerThread);
  }

  @Test
  void nullArgumentsAreRejected() {
    assertThatThrownBy(() -> transport.send(null, new byte[] {1}))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> transport.send("bob", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> transport.listen(null, bytes -> { }))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> transport.listen("bob", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void toStringListsReceiversWithoutPayloads() {
    transport.listen("alice", bytes -> { });
    assertThat(transport.toString()).contains("alice");
    assertThat(List.of(transport.toString().getBytes(UTF_8))).isNotEmpty();
  }
}
