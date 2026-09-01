package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.JobTask;
import dev.mintychochip.container.Payable;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Fired before a jobs payment is processed. */
public final class JobsPrePaymentEvent implements Cancellable, org.aincraft.api.event.Event {

  private final UUID playerId;
  private final Payable payable;
  private final Job job;
  private final JobTask jobTask;
  private boolean cancelled;

  /** Jobs pre payment event. */
  public JobsPrePaymentEvent(
      @NotNull UUID playerId,
      @NotNull Payable payable,
      @NotNull Job job,
      @NotNull JobTask jobTask) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.payable = payable;
    this.job = job;
    this.jobTask = jobTask;
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return playerId;
  }

  @Contract(pure = true)
  public @NotNull Payable getPayable() {
    return payable;
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return job;
  }

  @Contract(pure = true)
  public @NotNull JobTask getJobTask() {
    return jobTask;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
