package dev.mintychochip.event;

import dev.mintychochip.container.Payable;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Fired when a jobs payment is delivered. */
public final class JobsPaymentEvent implements Cancellable, org.aincraft.api.event.Event {

  private final UUID playerId;
  private final Payable base;
  private boolean cancelled;

  /** Jobs payment event. */
  public JobsPaymentEvent(@NotNull UUID playerId, @NotNull Payable base) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.base = base;
  }

  @Contract(pure = true)
  public @NotNull UUID getPlayerId() {
    return playerId;
  }

  @Contract(pure = true)
  public @NotNull Payable getBase() {
    return base;
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
