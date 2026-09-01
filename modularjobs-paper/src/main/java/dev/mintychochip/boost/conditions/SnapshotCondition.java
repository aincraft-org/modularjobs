package dev.mintychochip.boost.conditions;

import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Adapts a Paper-free {@link dev.mintychochip.databag.condition.Condition} onto the boost {@link
 * Condition} interface by reading {@link BoostContext#conditions()}.
 */
public record SnapshotCondition(@NotNull dev.mintychochip.databag.condition.Condition delegate)
    implements Condition {

  /** Wrap. */
  @Contract(pure = true)
  public static @NotNull Condition wrap(
      @NotNull dev.mintychochip.databag.condition.Condition delegate) {
    return new SnapshotCondition(delegate);
  }

  /** Unwraps a boost condition to the snapshot graph, wrapping lambdas as snapshot predicates. */
  @Contract(pure = true)
  public static @NotNull dev.mintychochip.databag.condition.Condition unwrap(
      @NotNull Condition condition) {
    if (condition instanceof SnapshotCondition snapshot) {
      return snapshot.delegate();
    }
    return ctx -> condition.applies(new BoostContext(null, null, null, null, null, ctx));
  }

  @Override
  @Contract(pure = true)
  public boolean applies(@NotNull BoostContext context) {
    ConditionContext snapshot = context.conditions();
    if (snapshot == null) {
      snapshot = ConditionContext.absent();
    }
    return delegate.test(snapshot);
  }
}
