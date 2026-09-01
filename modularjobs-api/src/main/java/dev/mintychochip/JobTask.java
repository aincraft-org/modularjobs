package dev.mintychochip;

import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Task definition owned by one {@link JobNode}. A descendant node may replace an inherited task
 * with the same {@linkplain #key() action-and-context key}.
 */
public record JobTask(
    @NotNull JobNodeKey nodeKey,
    @NotNull Key actionTypeKey,
    @NotNull Key contextKey,
    @NotNull List<Payable> payables) {

  /** Validates identifiers and stores an immutable payable list. */
  public JobTask {
    Objects.requireNonNull(nodeKey, "nodeKey");
    Objects.requireNonNull(actionTypeKey, "actionTypeKey");
    Objects.requireNonNull(contextKey, "contextKey");
    payables = List.copyOf(payables);
  }

  /** Returns all payables of the given type. */
  @Contract(pure = true)
  public @NotNull @UnmodifiableView List<Payable> payablesByType(@NotNull PayableType type) {
    return payables.stream().filter(p -> p.type().equals(type)).toList();
  }

  /** Returns the first payable of the given type, if present. */
  @Contract(pure = true)
  public @NotNull Optional<Payable> firstPayable(@NotNull PayableType type) {
    return payables.stream().filter(p -> p.type().equals(type)).findFirst();
  }

  /** Returns the action-and-context identity used for deterministic node inheritance. */
  @Contract(pure = true)
  public @NotNull TaskKey key() {
    return new TaskKey(actionTypeKey, contextKey);
  }

  /** Composite task identity within a resolved job-node path. */
  public record TaskKey(@NotNull Key actionTypeKey, @NotNull Key contextKey) {

    /** Validates the action-and-context identity. */
    public TaskKey {
      Objects.requireNonNull(actionTypeKey, "actionTypeKey");
      Objects.requireNonNull(contextKey, "contextKey");
    }

    /** As string. */
    @Contract(pure = true)
    public @NotNull String asString() {
      return actionTypeKey + "|" + contextKey;
    }
  }
}
