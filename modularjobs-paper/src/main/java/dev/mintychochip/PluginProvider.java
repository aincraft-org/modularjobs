package dev.mintychochip;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Paper-only access to the ModularJobs {@link JavaPlugin} instance for schedulers and similar. */
public final class PluginProvider {

  private static JavaPlugin plugin;

  private PluginProvider() {}

  /** Stores the plugin instance used by Paper-facing helpers. */
  public static void set(@Nullable JavaPlugin p) {
    plugin = p;
  }

  /** Returns the configured plugin instance or fails when bootstrap has not completed. */
  public static @NotNull JavaPlugin get() {
    if (plugin == null) {
      throw new IllegalStateException("Plugin not set");
    }
    return plugin;
  }
}
