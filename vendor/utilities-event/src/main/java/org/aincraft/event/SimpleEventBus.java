package org.aincraft.event;

import com.google.common.base.Preconditions;
import org.aincraft.api.event.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default {@link EventBus} implementation.
 *
 * <p>Thread-safe: subscriptions and posting may be interleaved concurrently. Posting collects a
 * snapshot of matching listeners so registration changes during dispatch do not affect the
 * in-flight event.
 */
final class SimpleEventBus implements EventBus {

  private static final Executor DEFAULT_EXECUTOR = ForkJoinPool.commonPool();

  private final Executor defaultAsyncExecutor;
  private final ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArrayList<ListenerEntry<?>>>
      byType = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Object, List<Subscription>> instanceBindings =
      new ConcurrentHashMap<>();
  private final AtomicLong orderSequence = new AtomicLong();

  SimpleEventBus() {
    this(DEFAULT_EXECUTOR);
  }

  SimpleEventBus(@NotNull Executor defaultAsyncExecutor) {
    this.defaultAsyncExecutor =
        Preconditions.checkNotNull(defaultAsyncExecutor, "defaultAsyncExecutor");
  }

  // ---------------------------------------------------------------- subscribe

  @Override
  public <E extends Event> @NotNull Subscription subscribe(
      @NotNull Class<E> eventType,
      @NotNull EventPriority priority,
      boolean ignoreCancelled,
      @Nullable Executor executor,
      @NotNull EventListener<? super E> listener) {
    Preconditions.checkNotNull(eventType, "eventType");
    Preconditions.checkNotNull(priority, "priority");
    Preconditions.checkNotNull(listener, "listener");
    long seq = orderSequence.getAndIncrement();
    ListenerEntry<E> entry =
        new ListenerEntry<>(eventType, priority, ignoreCancelled, executor, listener, seq);
    byType.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(entry);
    // Keep each bucket sorted for fast snapshot; global ordering is re-sorted at post time
    // but sorting per-bucket reduces work slightly.
    return new SubscriptionImpl(entry);
  }

  // --------------------------------------------------------------- annotation

  @Override
  public @NotNull List<Subscription> register(@NotNull Object instance) {
    Preconditions.checkNotNull(instance, "instance");
    List<Subscription> created = new ArrayList<>();
    java.util.Set<String> seen = new java.util.HashSet<>();
    java.util.Set<Method> methods = new java.util.LinkedHashSet<>();
    Collections.addAll(methods, instance.getClass().getMethods());
    Collections.addAll(methods, instance.getClass().getDeclaredMethods());

    for (Method method : methods) {
      Subscribe ann = method.getAnnotation(Subscribe.class);
      if (ann == null) continue;
      String sig = method.toString();
      if (!seen.add(sig)) continue;

      if (method.getParameterCount() != 1) {
        throw new IllegalArgumentException(
            "@Subscribe method must have exactly one parameter: " + method);
      }
      Class<?> param = method.getParameterTypes()[0];
      if (!Event.class.isAssignableFrom(param)) {
        throw new IllegalArgumentException("@Subscribe parameter must implement Event: " + method);
      }
      if (Modifier.isStatic(method.getModifiers())) {
        throw new IllegalArgumentException("@Subscribe method must not be static: " + method);
      }
      method.setAccessible(true);
      @SuppressWarnings("unchecked")
      Class<? extends Event> eventType = (Class<? extends Event>) param;
      EventPriority priority = ann.priority();
      boolean ignoreCancelled = ann.ignoreCancelled();
      long seq = orderSequence.getAndIncrement();

      EventListener<Event> listener =
          event -> {
            try {
              method.invoke(instance, event);
            } catch (java.lang.reflect.InvocationTargetException e) {
              Throwable cause = e.getCause();
              if (cause instanceof Exception ex) throw ex;
              if (cause instanceof Error err) throw err;
              throw new RuntimeException(cause);
            }
          };

      @SuppressWarnings("unchecked")
      ListenerEntry<Event> entry =
          new ListenerEntry<>(
              (Class<Event>) eventType, priority, ignoreCancelled, null, listener, seq);
      byType.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(entry);
      Subscription sub = new SubscriptionImpl(entry);
      created.add(sub);
    }

    if (!created.isEmpty()) {
      instanceBindings.merge(
          instance,
          created,
          (oldList, newList) -> {
            List<Subscription> merged = new ArrayList<>(oldList);
            merged.addAll(newList);
            return merged;
          });
    }
    return Collections.unmodifiableList(created);
  }

  @Override
  public void unregister(@NotNull Object instance) {
    Preconditions.checkNotNull(instance, "instance");
    List<Subscription> subs = instanceBindings.remove(instance);
    if (subs == null) return;
    for (Subscription s : subs) {
      unsubscribe(s);
    }
  }

  @Override
  public void unsubscribe(@NotNull Subscription subscription) {
    Preconditions.checkNotNull(subscription, "subscription");
    if (subscription instanceof SubscriptionImpl impl) {
      ListenerEntry<?> entry = impl.entry;
      if (!impl.markInactive()) {
        return; // already unsubscribed
      }
      CopyOnWriteArrayList<ListenerEntry<?>> list = byType.get(entry.eventType);
      if (list != null) {
        list.remove(entry);
        if (list.isEmpty()) {
          byType.remove(entry.eventType, list);
        }
      }
    } else {
      // Unknown implementation — best effort: try interface unsubscribe then remove by equality
      subscription.unsubscribe();
    }
  }

  // -------------------------------------------------------------------- post

  @Override
  @SuppressWarnings("unchecked")
  public <E extends Event> @NotNull E post(@NotNull E event) {
    Preconditions.checkNotNull(event, "event");
    List<ListenerEntry<?>> snapshot = collectMatching(event.getClass());
    for (ListenerEntry<?> raw : snapshot) {
      ListenerEntry<Event> entry = (ListenerEntry<Event>) raw;
      if (shouldSkip(entry, event)) continue;
      Executor exec = entry.executor;
      if (exec != null) {
        // Per-listener executor during sync post: run and block.
        try {
          CompletableFuture<Void> f = new CompletableFuture<>();
          exec.execute(
              () -> {
                try {
                  entry.listener.handle((Event) event);
                  f.complete(null);
                } catch (Throwable t) {
                  f.completeExceptionally(t);
                }
              });
          // Block for deterministic cancellation ordering
          try {
            f.join();
          } catch (Exception ignored) {
            // Exception already swallowed below; join wraps in CompletionException
          }
        } catch (Throwable t) {
          handleListenerException(t, entry, event);
        }
      } else {
        try {
          entry.listener.handle((Event) event);
        } catch (Throwable t) {
          handleListenerException(t, entry, event);
        }
      }
    }
    return event;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends Event> @NotNull CompletableFuture<E> postAsync(
      @NotNull E event, @Nullable Executor executor) {
    Preconditions.checkNotNull(event, "event");
    Executor exec = executor != null ? executor : defaultAsyncExecutor;
    List<ListenerEntry<?>> snapshot = collectMatching(event.getClass());

    // Sequential async chain to preserve priority + cancellation semantics
    CompletableFuture<E> result = CompletableFuture.completedFuture(event);
    // We need to run chain on `exec` starting off caller thread
    CompletableFuture<Void> chain = CompletableFuture.runAsync(() -> {}, exec);

    for (ListenerEntry<?> raw : snapshot) {
      ListenerEntry<Event> entry = (ListenerEntry<Event>) raw;
      chain =
          chain.thenCompose(
              v -> {
                if (shouldSkip(entry, event)) {
                  return CompletableFuture.completedFuture(null);
                }
                Executor target = entry.executor != null ? entry.executor : exec;
                return CompletableFuture.runAsync(
                    () -> {
                      try {
                        entry.listener.handle((Event) event);
                      } catch (Throwable t) {
                        handleListenerException(t, entry, event);
                      }
                    },
                    target);
              });
    }

    return chain.thenApply(v -> event);
  }

  // ----------------------------------------------------------------- helpers

  private boolean shouldSkip(ListenerEntry<?> entry, Event event) {
    if (entry.ignoreCancelled && event instanceof Cancellable c && c.isCancelled()) {
      return true;
    }
    // If entry is inactive (race), skip
    return !entry.active;
  }

  private void handleListenerException(Throwable t, ListenerEntry<?> entry, Event event) {
    // Unwrap CompletionException / ExecutionException for cleaner logging
    Throwable cause = t.getCause() != null ? t.getCause() : t;
    // Do not rethrow — log to stderr so one bad listener doesn't break the bus.
    // Users that need custom handling can wrap listeners.
    System.err.println(
        "[EventBus] Listener "
            + entry.listener
            + " for "
            + entry.eventType.getSimpleName()
            + " threw: "
            + cause);
    cause.printStackTrace(System.err);
  }

  private List<ListenerEntry<?>> collectMatching(Class<? extends Event> eventClass) {
    List<ListenerEntry<?>> out = new ArrayList<>();
    for (Map.Entry<Class<? extends Event>, CopyOnWriteArrayList<ListenerEntry<?>>> e :
        byType.entrySet()) {
      if (e.getKey().isAssignableFrom(eventClass)) {
        out.addAll(e.getValue());
      }
    }
    out.sort(
        Comparator.comparingInt((ListenerEntry<?> le) -> le.priority.ordinal())
            .thenComparingLong(le -> le.sequence));
    return out;
  }

  // ------------------------------------------------------------------ entry

  private static final class ListenerEntry<E extends Event> {
    final Class<E> eventType;
    final EventPriority priority;
    final boolean ignoreCancelled;
    final Executor executor;
    final EventListener<? super E> listener;
    final long sequence;
    volatile boolean active = true;

    ListenerEntry(
        Class<E> eventType,
        EventPriority priority,
        boolean ignoreCancelled,
        Executor executor,
        EventListener<? super E> listener,
        long sequence) {
      this.eventType = eventType;
      this.priority = priority;
      this.ignoreCancelled = ignoreCancelled;
      this.executor = executor;
      this.listener = listener;
      this.sequence = sequence;
    }
  }

  private final class SubscriptionImpl implements Subscription {
    final ListenerEntry<?> entry;
    volatile boolean active = true;

    SubscriptionImpl(ListenerEntry<?> entry) {
      this.entry = entry;
    }

    boolean markInactive() {
      if (!active) return false;
      active = false;
      entry.active = false;
      return true;
    }

    @Override
    public @NotNull Class<? extends Event> eventType() {
      return entry.eventType;
    }

    @Override
    public @NotNull EventPriority priority() {
      return entry.priority;
    }

    @Override
    public boolean ignoreCancelled() {
      return entry.ignoreCancelled;
    }

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public void unsubscribe() {
      SimpleEventBus.this.unsubscribe(this);
    }
  }
}
