package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Fired before experience is applied; listeners may cancel or mutate the amount. */
public final class JobExperienceGainEvent implements Cancellable {

  private final PlayerJobState state;
  private BigDecimal experienceGained;
  private boolean cancelled;

  /** Creates a cancellable experience change for the supplied player state. */
  public JobExperienceGainEvent(
      @NotNull PlayerJobState state, @NotNull BigDecimal experienceGained) {
    this.state = Objects.requireNonNull(state, "state");
    this.experienceGained = Objects.requireNonNull(experienceGained, "experienceGained");
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return state.job();
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
