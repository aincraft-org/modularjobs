package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Event fired when a player joins or rejoins a job. */
public final class JobJoinEvent implements org.aincraft.api.event.Event {

  private final Job job;
  private final PlayerJobState state;
  private final boolean rejoin;

  /** Creates a join transition for a complete aggregate and player job state. */
  public JobJoinEvent(@NotNull Job job, @NotNull PlayerJobState state, boolean rejoin) {
    this.job = Objects.requireNonNull(job, "job");
    this.state = Objects.requireNonNull(state, "state");
    if (!state.belongsTo(job)) {
      throw new IllegalArgumentException("Player state does not belong to the supplied job");
    }
    this.rejoin = rejoin;
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return state.playerId();
  }

  @Contract(pure = true)
  public @NotNull Job getJob() {
    return job;
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
