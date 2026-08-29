package dev.mintychochip.paper.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.aincraft.api.event.Event;
import org.aincraft.api.event.Subscribe;
import org.aincraft.event.EventBuses;
import org.junit.jupiter.api.Test;

class LifecycleEventBusTest {

  @Test
  void synchronousFailureStopsLaterListenersAndPropagates() {
    LifecycleEventBus bus = new LifecycleEventBus(EventBuses.create());
    try {
      IllegalStateException expected = new IllegalStateException("listener failure");
      int[] calls = {0, 0};
      bus.subscribe(
          TestEvent.class,
          event -> {
            calls[0]++;
            throw expected;
          });
      bus.subscribe(TestEvent.class, event -> calls[1]++);

      IllegalStateException actual =
          assertThrows(IllegalStateException.class, () -> bus.post(new TestEvent()));

      assertSame(expected, actual);
      assertEquals(1, calls[0]);
      assertEquals(0, calls[1]);
    } finally {
      bus.close();
    }
  }

  @Test
  void asynchronousFailureCompletesFutureExceptionally() {
    LifecycleEventBus bus = new LifecycleEventBus(EventBuses.create());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      IllegalArgumentException expected = new IllegalArgumentException("async listener failure");
      bus.subscribe(
          TestEvent.class,
          event -> {
            throw expected;
          });

      CompletionException actual =
          assertThrows(
              CompletionException.class, () -> bus.postAsync(new TestEvent(), executor).join());

      assertSame(expected, actual.getCause());
    } finally {
      bus.close();
      executor.shutdownNow();
    }
  }

  @Test
  void annotationRegistrationUsesTheSamePropagationPath() {
    LifecycleEventBus bus = new LifecycleEventBus(EventBuses.create());
    try {
      AnnotatedListener listener = new AnnotatedListener();
      bus.register(listener);

      RuntimeException actual =
          assertThrows(RuntimeException.class, () -> bus.post(new TestEvent()));

      assertSame(listener.failure, actual);
      assertEquals(1, listener.calls);
    } finally {
      bus.close();
    }
  }

  @Test
  void repeatedRegistrationUnregistersEverySubscription() {
    LifecycleEventBus bus = new LifecycleEventBus(EventBuses.create());
    try {
      AnnotatedListener listener = new AnnotatedListener(false);
      bus.register(listener);
      bus.register(listener);
      bus.unregister(listener);

      bus.post(new TestEvent());

      assertEquals(0, listener.calls);
    } finally {
      bus.close();
    }
  }

  @Test
  void closeRejectsRetainedBusOperations() {
    LifecycleEventBus bus = new LifecycleEventBus(EventBuses.create());
    var subscription = bus.subscribe(TestEvent.class, event -> {});
    bus.close();

    assertThrows(IllegalStateException.class, () -> bus.post(new TestEvent()));
    assertThrows(IllegalStateException.class, () -> bus.subscribe(TestEvent.class, event -> {}));
    assertThrows(IllegalStateException.class, () -> bus.unsubscribe(subscription));
  }

  private static final class TestEvent implements Event {}

  private static final class AnnotatedListener {
    private final RuntimeException failure;
    private int calls;

    private AnnotatedListener() {
      this(true);
    }

    private AnnotatedListener(boolean fail) {
      this.failure = fail ? new RuntimeException("annotated listener failure") : null;
    }

    @Subscribe
    public void onEvent(TestEvent event) {
      calls++;
      if (failure != null) {
        throw failure;
      }
    }
  }
}
