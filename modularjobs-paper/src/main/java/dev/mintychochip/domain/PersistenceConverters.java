package dev.mintychochip.domain;

import dev.mintychochip.Job;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.Currency;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.model.JobTaskRecord;
import dev.mintychochip.domain.model.JobTreeRecord;
import dev.mintychochip.domain.model.PayableRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for converting between domain objects and persistence records. This lives in paper
 * to avoid circular dependencies with api.
 */
public final class PersistenceConverters {

  private PersistenceConverters() {}

  /** Converts to record. */
  @Contract(pure = true)
  public static @NotNull JobTreeRecord toRecord(@NotNull Job job) {
    if (!(job instanceof JobImpl jobImpl)) {
      throw new IllegalArgumentException("Job must be a JobImpl instance");
    }
    return jobImpl.toRecord();
  }

  /** Converts to record. */
  @Contract(pure = true)
  public static @NotNull PlayerJobStateRecord toRecord(@NotNull PlayerJobState state) {
    Job job = state.job();
    JobNode currentNode = state.currentNode();
    if (!job.jobKey().equals(currentNode.jobKey())
        || !currentNode.equals(job.node(currentNode.nodeKey()))) {
      throw new IllegalArgumentException("Current node does not belong to the job tree");
    }
    return new PlayerJobStateRecord(
        state.playerId().toString(),
        job.jobKey().asString(),
        currentNode.nodeKey().asString(),
        state.experience());
  }

  /** Converts to record. */
  @Contract(pure = true)
  public static @NotNull JobTaskRecord toRecord(@NotNull JobTask task) {
    return new JobTaskRecord(
        task.nodeKey().asString(),
        task.actionTypeKey().toString(),
        task.contextKey().toString(),
        task.payables().stream().map(PersistenceConverters::toRecord).collect(Collectors.toList()));
  }

  /** Converts to record. */
  @Contract(pure = true)
  public static @NotNull PayableRecord toRecord(@NotNull Payable payable) {
    Currency currency = payable.amount().currency().orElse(null);
    return new PayableRecord(
        payable.type().key().toString(),
        payable.amount().value(),
        currency == null ? null : currency.identifier(),
        currency == null ? null : currency.symbol());
  }

  /** From record. */
  @Contract(pure = true)
  public static @NotNull PlayerJobState fromRecord(
      @NotNull PlayerJobStateRecord record, @NotNull Job job) {
    return PlayerJobStateImpl.fromRecord(record, job);
  }

  /** From record. */
  @Contract(pure = true)
  public static @NotNull JobTask fromRecord(
      @NotNull JobTaskRecord record, @NotNull Function<String, PayableType> typeResolver) {
    List<PayableRecord> payables = record.payables() == null ? List.of() : record.payables();
    return new JobTask(
        new JobNodeKey(Key.key(record.nodeKey())),
        Key.key(record.actionTypeKey()),
        Key.key(record.contextKey()),
        payables.stream().map(p -> fromRecord(p, typeResolver)).collect(Collectors.toList()));
  }

  /** From record. */
  @Contract(pure = true)
  public static @NotNull Payable fromRecord(
      @NotNull PayableRecord record, @NotNull Function<String, PayableType> typeResolver) {
    NamespacedKey key = NamespacedKey.fromString(record.payableTypeKey());
    if (key == null) {
      throw new IllegalArgumentException("Invalid payable type key: " + record.payableTypeKey());
    }
    PayableType type = typeResolver.apply(record.payableTypeKey());
    String currencyIdentifier = record.currencyIdentifier();
    String currencySymbol = record.currencySymbol();
    PayableAmount amount =
        currencyIdentifier != null
            ? PayableAmount.create(
                record.amount(),
                Currency.of(
                    currencyIdentifier,
                    currencySymbol == null ? currencyIdentifier : currencySymbol))
            : PayableAmount.create(record.amount());
    return new Payable(type, amount);
  }
}
