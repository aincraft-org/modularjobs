package dev.mintychochip.action;

import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.payment.JobsPaymentHandler;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.service.ActionService;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/** Default action registration and reporting service. */
public final class ActionServiceImpl implements ActionService {

  private final Registry<ActionType> registry;
  private final JobsPaymentHandler paymentHandler;

  /** Creates a service backed by the live action registry and payment pipeline. */
  public ActionServiceImpl(
      @NotNull Registry<ActionType> registry, @NotNull JobsPaymentHandler paymentHandler) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.paymentHandler = Objects.requireNonNull(paymentHandler, "paymentHandler");
  }

  @Override
  @SuppressWarnings("PMD.AvoidSynchronizedStatement")
  public @NotNull ActionType register(@NotNull Key key, @NotNull String name) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    // Registry identity is the shared lock, so separate service instances cannot race.
    synchronized (registry) {
      if (registry.isRegistered(key)) {
        throw new IllegalArgumentException("action type already registered: " + key);
      }
      ActionType action = new ActionTypeImpl(name, key);
      registry.register(action);
      return action;
    }
  }

  @Override
  public void report(@NotNull UUID playerId, @NotNull ActionType type, @NotNull Context context) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(context, "context");
    Key typeKey = Objects.requireNonNull(type.key(), "type.key");
    if (!registry.isRegistered(typeKey)) {
      throw new IllegalArgumentException("action type is not registered: " + typeKey);
    }
    ActionType registered = registry.getOrThrow(typeKey);
    paymentHandler.pay(Bukkit.getOfflinePlayer(playerId), registered, context);
  }
}
