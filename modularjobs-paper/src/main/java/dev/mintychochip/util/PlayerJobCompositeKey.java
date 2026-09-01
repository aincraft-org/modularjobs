package dev.mintychochip.util;

import dev.mintychochip.Job;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Compound identity for a player and a job.
 *
 * @param playerId player UUID
 * @param jobKey namespaced job key
 */
public record PlayerJobCompositeKey(@NotNull UUID playerId, @NotNull Key jobKey) {

  /**
   * Creates a key from an offline player and a job.
   *
   * @param player player whose UUID is used
   * @param job job whose namespaced key is used
   * @return compound key
   */
  @Contract(pure = true)
  public static @NotNull PlayerJobCompositeKey create(
      @NotNull OfflinePlayer player, @NotNull Job job) {
    return new PlayerJobCompositeKey(player.getUniqueId(), job.key());
  }
}
