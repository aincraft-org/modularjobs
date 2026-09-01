package dev.mintychochip.payable;

import dev.mintychochip.container.PayableAmount;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Optional adapter from ModularJobs economy rewards to a registered Vault economy. */
public final class VaultEconomyProvider implements dev.mintychochip.container.EconomyProvider {

  private static final Logger LOGGER = Logger.getLogger(VaultEconomyProvider.class.getName());

  private final Economy economy;

  VaultEconomyProvider(@NotNull Economy economy) {
    this.economy = Objects.requireNonNull(economy, "economy");
  }

  static @Nullable VaultEconomyProvider tryCreate() {
    if (Bukkit.getServicesManager() == null) {
      return null;
    }
    RegisteredServiceProvider<Economy> registration =
        Bukkit.getServicesManager().getRegistration(Economy.class);
    return registration == null ? null : new VaultEconomyProvider(registration.getProvider());
  }

  @Override
  public boolean isCurrencySupported() {
    return economy.isEnabled();
  }

  @Override
  public boolean deposit(@NotNull UUID playerId, @Nullable PayableAmount payableAmount) {
    if (payableAmount == null) {
      return false;
    }
    BigDecimal amount = payableAmount.value();
    if (amount.signum() <= 0) {
      return false;
    }

    double vaultAmount = amount.doubleValue();
    if (!Double.isFinite(vaultAmount) || BigDecimal.valueOf(vaultAmount).compareTo(amount) != 0) {
      return false;
    }

    try {
      EconomyResponse response =
          economy.depositPlayer(Bukkit.getOfflinePlayer(playerId), vaultAmount);
      if (response != null && response.transactionSuccess()) {
        return true;
      }
      LOGGER.warning(
          "Vault deposit rejected for "
              + playerId
              + (response == null || response.errorMessage == null
                  ? ""
                  : ": " + response.errorMessage));
      return false;
    } catch (IllegalStateException exception) {
      LOGGER.log(Level.SEVERE, "Vault deposit failed for player " + playerId, exception);
      return false;
    }
  }
}
