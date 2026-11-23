package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Fired before experience is applied; listeners may cancel or mutate the amount. */
public final class JobExperienceGainEvent implements Cancellable, org.aincraft.api.event.Event {

  private final Job job;
  private final PlayerJobState state;
  private BigDecimal experienceGained;
  private boolean cancelled;

  /** Creates a cancellable experience change for the supplied aggregate and player state. */
  public JobExperienceGainEvent(
      @NotNull Job job, @NotNull PlayerJobState state, @NotNull BigDecimal experienceGained) {
    this.job = Objects.requireNonNull(job, "job");
    this.state = Objects.requireNonNull(state, "state");
    if (!state.belongsTo(job)) {
      throw new IllegalArgumentException("Player state does not belong to the supplied job");
    }
    this.experienceGained = Objects.requireNonNull(experienceGained, "experienceGained");
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return job;
  }

  @Contract(pure = true)
  public @NotNull PlayerJobState getPlayerJobState() {
    return state;
  }

  @Contract(pure = true)
  public @NotNull BigDecimal getExperienceGained() {
    return experienceGained;
  }

  public void setExperienceGained(@NotNull BigDecimal experienceGained) {
    this.experienceGained = Objects.requireNonNull(experienceGained, "experienceGained");
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
