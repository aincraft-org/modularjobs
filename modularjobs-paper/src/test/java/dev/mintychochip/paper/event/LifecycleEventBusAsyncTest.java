package dev.mintychochip.paper.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.aincraft.api.event.Event;
import org.aincraft.api.event.EventPriority;
import org.aincraft.event.EventBuses;
import org.junit.jupiter.api.Test;

class LifecycleEventBusAsyncTest {

  @Test
  void asyncDispatchDoesNotDeadlockWhenListenerUsesSameExecutor() throws Exception {
    LifecycleEventBus bus = new LifecycleEventBus(EventBuses.create());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    TestEvent event = new TestEvent();
    int[] calls = {0};
    try {
      bus.subscribe(TestEvent.class, EventPriority.NORMAL, false, executor, ignored -> calls[0]++);

      assertSame(event, bus.postAsync(event, executor).get(1, TimeUnit.SECONDS));
      assertEquals(1, calls[0]);
    } finally {
      bus.close();
      executor.shutdownNow();
    }
  }

  private static final class TestEvent implements Event {}
}
