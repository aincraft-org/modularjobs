package dev.mintychochip.payable;

import dev.mintychochip.container.EconomyProvider;
import java.util.Locale;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Selects the optional economy provider used by payable wiring. */
public final class EconomyProviderFactory {

  private static final String MINT2_PLUGIN = "Mint2";
  private static final String VAULT_PLUGIN = "Vault";
  private static final String REQUIRED_KEY = "economy.required";
  private static final String MISSING_PROVIDER_KEY = "economy.missing-provider";

  private EconomyProviderFactory() {}

  /**
   * Selects Mint2 when enabled, otherwise a registered Vault economy.
   *
   * <p>Mint2 wins before its service registers because its bridge resolves that service lazily at
   * deposit time. Vault is considered only when Mint2 is absent.
   */
  public static @Nullable EconomyProvider tryCreate(@NotNull Plugin plugin) {
    PluginManager pluginManager = plugin.getServer().getPluginManager();
    if (pluginManager.isPluginEnabled(MINT2_PLUGIN)) {
      return new MintEconomyProvider();
    }
    if (!pluginManager.isPluginEnabled(VAULT_PLUGIN)) {
      return null;
    }
    return VaultEconomyProvider.tryCreate();
  }

  /**
   * Whether the legacy required flag is enabled.
   *
   * <p>The generated default is false. When true and no explicit {@code economy.missing-provider}
   * policy is configured, the factory preserves the old fail-fast behavior.
   */
  public static boolean isRequired(@NotNull Plugin plugin) {
    return plugin.getConfig().getBoolean(REQUIRED_KEY, false);
  }

  /**
   * Resolves the configured provider, defaulting to a non-throwing blackhole fallback.
   *
   * @throws IllegalStateException when the configured policy is {@code fail} and no provider exists
   */
  public static @NotNull EconomyProvider createOrFail(@NotNull Plugin plugin) {
    EconomyProvider provider = tryCreate(plugin);
    if (provider != null) {
      return provider;
    }

    return switch (missingProviderPolicy(plugin)) {
      case BLACKHOLE -> new BlackholeEconomyProvider(plugin);
      case FAIL ->
          throw new IllegalStateException(
              "No economy provider is available. Install Mint2 or Vault, or set "
                  + MISSING_PROVIDER_KEY
                  + ": blackhole in config.yml.");
    };
  }

  private static @NotNull MissingProviderPolicy missingProviderPolicy(@NotNull Plugin plugin) {
    String configured =
        plugin.getConfig().contains(MISSING_PROVIDER_KEY, true)
            ? plugin.getConfig().getString(MISSING_PROVIDER_KEY)
            : null;
    if (configured != null) {
      return switch (configured.trim().toLowerCase(Locale.ROOT)) {
        case "blackhole" -> MissingProviderPolicy.BLACKHOLE;
        case "fail" -> MissingProviderPolicy.FAIL;
        default ->
            throw new IllegalArgumentException(
                "Unknown "
                    + MISSING_PROVIDER_KEY
                    + " value '"
                    + configured
                    + "'; expected blackhole or fail.");
      };
    }
    return isRequired(plugin) ? MissingProviderPolicy.FAIL : MissingProviderPolicy.BLACKHOLE;
  }

  private enum MissingProviderPolicy {
    BLACKHOLE,
    FAIL
  }
}
