package dev.mintychochip.container;

import dev.mintychochip.Bridge;
import dev.mintychochip.container.boost.factories.BoostFactory;
import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Boost. */
@FunctionalInterface
public interface Boost {

  /** Boost. */
  @Contract(pure = true)
  @NotNull
  BigDecimal boost(@NotNull BigDecimal amount);

  /** Lazy factory access — avoids class-init dependency on Bridge/Bukkit. */
  static @NotNull BoostFactory factory() {
    return Bridge.bridge().boostFactory();
  }
}
