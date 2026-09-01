package dev.mintychochip;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/** API-local runtime holder populated by the Paper bootstrap. */
final class BridgeRuntime {

  private static volatile Bridge instance;

  private BridgeRuntime() {}

  static @NotNull Bridge get() {
    Bridge current = instance;
    if (current == null) {
      throw new IllegalStateException("Bridge not registered (plugin not enabled)");
    }
    return current;
  }

  static void register(@NotNull Bridge bridge) {
    instance = Objects.requireNonNull(bridge, "bridge");
  }

  static void unregister() {
    instance = null;
  }
}
