package dev.mintychochip;

import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Strongly typed key identifying a node within a job tree. */
public record JobNodeKey(@NotNull Key key) implements Keyed {

  /** Rejects a null underlying Adventure key. */
  public JobNodeKey {
    Objects.requireNonNull(key, "key");
  }

  /** Returns the namespaced string form used at configuration and persistence boundaries. */
  @Contract(pure = true)
  public @NotNull String asString() {
    return key.asString();
  }
}
