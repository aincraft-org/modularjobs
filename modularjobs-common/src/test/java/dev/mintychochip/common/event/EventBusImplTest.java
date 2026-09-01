package dev.mintychochip.common.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.event.EventBus;
import dev.mintychochip.event.JobsPaymentEvent;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventBusImplTest {

  @Test
  void publishesSynchronouslyToSubscribers() {
    EventBus bus = new EventBusImpl();
    AtomicReference<Object> received = new AtomicReference<>();
    bus.subscribe(received::set);
    Object event = new Object();

    Object published = bus.publish(event);

    assertSame(event, published);
    assertSame(event, received.get());
  }

  @Test
  void listenersCanMutatePublishedEventsSynchronously() {
    EventBus bus = new EventBusImpl();
    bus.subscribe(
        event -> {
          if (event instanceof JobsPaymentEvent payment) {
            payment.setCancelled(true);
          }
        });
    JobsPaymentEvent event = new JobsPaymentEvent(UUID.randomUUID(), null);

    assertFalse(event.isCancelled());
    bus.publish(event);

    assertTrue(event.isCancelled());
  }

  @Test
  void rejectsNullSubscribers() {
    EventBus bus = new EventBusImpl();

    assertThrows(NullPointerException.class, () -> bus.subscribe(null));
  }

  @Test
  void rejectsNullEvents() {
    EventBus bus = new EventBusImpl();

    assertThrows(NullPointerException.class, () -> bus.publish(null));
  }
}
