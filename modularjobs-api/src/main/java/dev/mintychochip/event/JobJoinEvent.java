package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Event fired when a player joins or rejoins a job. */
public final class JobJoinEvent implements org.aincraft.api.event.Event {

  private final PlayerJobState state;
  private final boolean rejoin;

  /** Creates a join transition for a complete player job state. */
  public JobJoinEvent(@NotNull PlayerJobState state, boolean rejoin) {
    this.state = Objects.requireNonNull(state, "state");
    this.rejoin = rejoin;
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return state.job();
  }

  /** Returns the joined state, including its active specialization node. */
  @Contract(pure = true)
  public @NotNull PlayerJobState getPlayerJobState() {
    return state;
  }

  /** Whether this is a rejoin (player previously left this job). */
  public boolean isRejoin() {
    return rejoin;
  }
}
