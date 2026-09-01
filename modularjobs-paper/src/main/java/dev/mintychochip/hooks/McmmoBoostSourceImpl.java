package dev.mintychochip.hooks;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.BoostSource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Supplies multiplicative boosts while a player has an active McMMO super ability.
 *
 * <p>The source tracks each player's active ability and uses the configured amount for that ability
 * when evaluating a boost context.
 */
public class McmmoBoostSourceImpl implements BoostSource {

  private final Map<UUID, SuperAbilityType> activeAbilities;

  private final Map<SuperAbilityType, BigDecimal> boostAmounts;

  /**
   * Creates a source backed by the supplied active-ability map and boost amounts.
   *
   * @param activeAbilities map containing each tracked player's active super ability
   * @param boostAmounts configured multiplicative boost amount for each super ability
   */
  public McmmoBoostSourceImpl(
      @NotNull Map<UUID, SuperAbilityType> activeAbilities,
      @NotNull Map<SuperAbilityType, BigDecimal> boostAmounts) {
    this.activeAbilities = activeAbilities;
    this.boostAmounts = boostAmounts;
  }

  /**
   * Creates and registers a McMMO boost source.
   *
   * <p>The source uses an in-memory map and registers a controller with the supplied plugin to
   * track McMMO ability activation and deactivation events.
   *
   * @param plugin plugin with which the event controller is registered
   * @param boostAmounts configured multiplicative boost amount for each super ability
   * @return a source that evaluates boosts for currently active McMMO abilities
   */
  public static @NotNull McmmoBoostSourceImpl create(
      @NotNull Plugin plugin, @NotNull Map<SuperAbilityType, BigDecimal> boostAmounts) {
    Map<UUID, SuperAbilityType> activeAbilities = new HashMap<>();
    Bukkit.getPluginManager().registerEvents(new McmmoController(activeAbilities), plugin);
    return new McmmoBoostSourceImpl(activeAbilities, boostAmounts);
  }

  /**
   * Evaluates the configured boost for the context's player.
   *
   * <p>Returns no boosts when the player has no tracked active ability or when that ability has no
   * configured amount. Otherwise, returns one multiplicative boost using the configured amount.
   *
   * @param context context containing the player whose active ability is evaluated
   * @return an empty list or a single configured multiplicative boost
   */
  @Override
  public @NotNull List<Boost> evaluate(@NotNull BoostContext context) {
    UUID playerId = context.playerId();
    if (!activeAbilities.containsKey(playerId)) {
      return List.of();
    }
    SuperAbilityType type = activeAbilities.get(playerId);
    BigDecimal amount = boostAmounts.get(type);
    if (amount == null) {
      return List.of();
    }
    return List.of(Boost.factory().multiplicative(amount));
  }

  /**
   * Listens for McMMO super ability lifecycle events and updates the active-ability map.
   *
   * @param activeAbilities map updated when players activate or deactivate super abilities
   */
  public record McmmoController(@NotNull Map<UUID, SuperAbilityType> activeAbilities)
      implements Listener {

    /** Stores the ability just activated by the player. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAbilityOn(@NotNull final McMMOPlayerAbilityActivateEvent event) {
      activeAbilities.put(event.getPlayer().getUniqueId(), event.getAbility());
    }

    /** Removes the player's active ability on deactivation. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAbilityOff(@NotNull final McMMOPlayerAbilityDeactivateEvent event) {
      activeAbilities.remove(event.getPlayer().getUniqueId());
    }
  }

  /**
   * Returns the stable identifier for this McMMO boost source.
   *
   * @return this source's namespaced key
   */
  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:mcmmo_source");
  }

  /**
   * Returns the human-readable description of this source's behavior.
   *
   * @return description indicating that boosts are active during McMMO super abilities
   */
  @Override
  public @NotNull String description() {
    return "McMMO super ability boosts - Active during McMMO super abilities";
  }
}
