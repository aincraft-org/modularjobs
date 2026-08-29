package dev.mintychochip.paper.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.aincraft.api.event.Event;
import org.aincraft.api.event.EventBus;
import org.aincraft.api.event.EventListener;
import org.aincraft.api.event.EventPriority;
import org.aincraft.api.event.Subscribe;
import org.aincraft.api.event.Subscription;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin-owned event bus that unregisters every subscription when the plugin disables.
 *
 * <p>All synchronous posts remain on the caller thread; asynchronous dispatch is only used when a
 * caller explicitly invokes {@link #postAsync(Event)}.
 *
 * <p>The legacy ModularJobs bridge propagated listener failures. This wrapper records failures from
 * utility listeners, including listeners dispatched on a per-listener executor, and rethrows the
 * first failure after the utility bus finishes its ordered dispatch.
 */
public final class LifecycleEventBus implements EventBus, AutoCloseable {

  private final EventBus delegate;
  private final Set<Subscription> subscriptions = ConcurrentHashMap.newKeySet();
  private final Map<Object, Set<Subscription>> instanceSubscriptions = new ConcurrentHashMap<>();
  private final Map<Event, DispatchState> dispatchStates =
      Collections.synchronizedMap(new IdentityHashMap<>());
  private volatile boolean closed;

  /** Creates a lifecycle wrapper around the utility event bus implementation. */
  public LifecycleEventBus(@NotNull EventBus delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public synchronized <E extends Event> @NotNull Subscription subscribe(
      @NotNull Class<E> eventType,
      @NotNull EventPriority priority,
      boolean ignoreCancelled,
      Executor executor,
      @NotNull EventListener<? super E> listener) {
    ensureOpen();
    EventListener<E> guardedListener =
        event -> {
          DispatchState state = dispatchStates.get(event);
          if (state == null) {
            listener.handle(event);
            return;
          }
          state.invoke(listener, event);
        };
    Subscription subscription =
        delegate.subscribe(eventType, priority, ignoreCancelled, executor, guardedListener);
    subscriptions.add(subscription);
    return subscription;
  }

  @Override
  public synchronized @NotNull List<Subscription> register(@NotNull Object instance) {
    ensureOpen();
    Objects.requireNonNull(instance, "instance");
    List<Subscription> created = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    Set<Method> methods = new LinkedHashSet<>();
    Collections.addAll(methods, instance.getClass().getMethods());
    Collections.addAll(methods, instance.getClass().getDeclaredMethods());

    try {
      for (Method method : methods) {
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        if (annotation == null || !seen.add(method.toString())) {
          continue;
        }
        if (method.getParameterCount() != 1) {
          throw new IllegalArgumentException(
              "@Subscribe method must have exactly one parameter: " + method);
        }
        Class<?> parameterType = method.getParameterTypes()[0];
        if (!Event.class.isAssignableFrom(parameterType)) {
          throw new IllegalArgumentException(
              "@Subscribe parameter must implement Event: " + method);
        }
        if (Modifier.isStatic(method.getModifiers())) {
          throw new IllegalArgumentException("@Subscribe method must not be static: " + method);
        }
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Class<Event> eventType = (Class<Event>) parameterType;
        EventListener<Event> listener =
            event -> {
              try {
                method.invoke(instance, event);
              } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof Exception checked) {
                  throw checked;
                }
                if (cause instanceof Error error) {
                  throw error;
                }
                throw new RuntimeException(cause);
              }
            };
        created.add(
            subscribe(
                eventType, annotation.priority(), annotation.ignoreCancelled(), null, listener));
      }
    } catch (RuntimeException | Error failure) {
      created.forEach(this::unsubscribe);
      throw failure;
    }

    if (!created.isEmpty()) {
      subscriptions.addAll(created);
      instanceSubscriptions.compute(
          instance,
          (key, previous) -> {
            Set<Subscription> merged = new HashSet<>();
            if (previous != null) {
              merged.addAll(previous);
            }
            merged.addAll(created);
            return Set.copyOf(merged);
          });
    }
    return Collections.unmodifiableList(created);
  }

  @Override
  public synchronized void unregister(@NotNull Object instance) {
    ensureOpen();
    Set<Subscription> registered = instanceSubscriptions.remove(instance);
    if (registered == null) {
      return;
    }
    registered.forEach(this::unsubscribe);
  }

  @Override
  public synchronized void unsubscribe(@NotNull Subscription subscription) {
    ensureOpen();
    subscriptions.remove(subscription);
    delegate.unsubscribe(subscription);
  }

  @Override
  public <E extends Event> @NotNull E post(@NotNull E event) {
    ensureOpen();
    DispatchState state = new DispatchState();
    dispatchStates.put(event, state);
    E posted;
    try {
      posted = delegate.post(event);
    } catch (RuntimeException | Error failure) {
      dispatchStates.remove(event);
      Throwable listenerFailure = state.failure();
      if (listenerFailure != null) {
        throw propagate(listenerFailure);
      }
      throw failure;
    }
    dispatchStates.remove(event);
    Throwable failure = state.failure();
    if (failure != null) {
      throw propagate(failure);
    }
    return posted;
  }

  @Override
  public <E extends Event> @NotNull CompletableFuture<E> postAsync(
      @NotNull E event, Executor executor) {
    ensureOpen();
    DispatchState state = new DispatchState();
    dispatchStates.put(event, state);
    CompletableFuture<E> delegateFuture;
    try {
      delegateFuture = delegate.postAsync(event, executor);
    } catch (RuntimeException | Error failure) {
      dispatchStates.remove(event);
      throw failure;
    }
    return delegateFuture.handle(
        (posted, failure) -> {
          dispatchStates.remove(event);
          Throwable listenerFailure = state.failure();
          if (listenerFailure != null) {
            throw propagate(listenerFailure);
          }
          if (failure != null) {
            if (failure instanceof CompletionException completion) {
              throw completion;
            }
            throw new CompletionException(failure);
          }
          return posted;
        });
  }

  /** Unregisters all subscriptions and rejects further bus operations. */
  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    for (Subscription subscription : subscriptions) {
      delegate.unsubscribe(subscription);
    }
    subscriptions.clear();
    instanceSubscriptions.clear();
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("event bus is closed");
    }
  }

  private static RuntimeException propagate(Throwable failure) {
    if (failure instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    if (failure instanceof Error error) {
      throw error;
    }
    return new RuntimeException(failure);
  }

  private static final class DispatchState {
    private Throwable failure;

    synchronized <E extends Event> void invoke(EventListener<? super E> listener, E event) {
      if (failure != null) {
        return;
      }
      try {
        listener.handle(event);
      } catch (Throwable thrown) {
        failure = thrown;
      }
    }

    synchronized Throwable failure() {
      return failure;
    }
  }
}
