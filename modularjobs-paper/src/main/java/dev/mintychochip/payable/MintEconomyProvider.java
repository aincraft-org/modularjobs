package dev.mintychochip.payable;

import dev.mintychochip.MintException;
import dev.mintychochip.Results;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.economy.AccountId;
import dev.mintychochip.economy.AccountService;
import dev.mintychochip.economy.Actor;
import dev.mintychochip.economy.ActorSource;
import dev.mintychochip.economy.ActorSources;
import dev.mintychochip.economy.Currency;
import dev.mintychochip.economy.CurrencyService;
import dev.mintychochip.economy.EconomyProvider;
import dev.mintychochip.economy.MonetaryAmount;
import dev.mintychochip.economy.MonetaryAmounts;
import dev.mintychochip.economy.PlayerId;
import dev.mintychochip.economy.TransferResult;
import dev.mintychochip.economy.Transfers;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Mint2 ledger bridge.
 *
 * <p>Resolves Mint2's {@link EconomyProvider} service lazily because Mint2 registers it
 * asynchronously during startup. Payments mint into the default currency, mirroring Mint2's own
 * Vault adapter (a deposit leg by the system vault actor).
 *
 * <p>This class depends only on the published Mint2 API and never on platform-internal packages.
 */
public final class MintEconomyProvider implements dev.mintychochip.container.EconomyProvider {

  private static final Logger LOGGER = Logger.getLogger(MintEconomyProvider.class.getName());

  /** The Mint2 plugin name (paper-plugin.yml). */
  private static final String MINT2_PLUGIN = "Mint2";

  /** Visible for selection tests; callers go through {@link EconomyProviderFactory}. */
  public MintEconomyProvider() {}

  @Override
  public boolean isCurrencySupported() {
    return resolve() != null;
  }

  @Override
  public boolean deposit(@NotNull UUID playerId, @Nullable PayableAmount payableAmount) {
    BigDecimal amount = payableAmount == null ? null : payableAmount.value();
    if (amount == null || amount.signum() <= 0) {
      return false;
    }

    EconomyProvider mint = resolve();
    if (mint == null) {
      return false;
    }
    try {
      return deposit(mint, playerId, amount);
    } catch (MintException | IllegalStateException e) {
      LOGGER.log(Level.SEVERE, "Mint2 deposit failed for player " + playerId, e);
      return false;
    }
  }

  private static boolean deposit(
      @NotNull EconomyProvider mint, @NotNull UUID playerId, @NotNull BigDecimal amount) {
    AccountService accounts = mint.accountService();
    CurrencyService currencies = mint.currencyService();
    ActorSources actors = mint.actors();
    ActorSource.Core core = actors == null ? null : actors.core();
    if (accounts == null || currencies == null || core == null) {
      throw new IllegalStateException("Mint2 provider is missing required services");
    }
    Currency currency = Results.take(currencies.getDefault());
    if (currency == null) {
      throw new IllegalStateException("Mint2 has no default currency configured");
    }
    AccountId account = ensurePlayerAccount(accounts, core, playerId);
    MonetaryAmount value = MonetaryAmounts.of(amount, currency);
    TransferResult outcome =
        Results.take(
            accounts.apply(core.vault(), Transfers.builder().deposit(account, value).build()));
    if (outcome instanceof TransferResult.Ok) {
      LOGGER.fine("Mint2 deposited " + amount + " to " + playerId);
      return true;
    }
    if (outcome instanceof TransferResult.Error error) {
      LOGGER.warning(
          "Mint2 deposit rejected for "
              + playerId
              + ": "
              + error.code()
              + (error.message() == null ? "" : " " + error.message()));
    }
    return false;
  }

  private static @NotNull AccountId ensurePlayerAccount(
      @NotNull AccountService accounts, @NotNull ActorSource.Core core, @NotNull UUID playerId) {
    AccountId id = new AccountId(playerId, AccountId.Type.PLAYER);
    Actor owner = core.player(new PlayerId(playerId));
    try {
      Results.take(accounts.createAccount(id, owner));
    } catch (MintException e) {
      if (e.errorCode() != MintException.ErrorCode.DUPLICATE) {
        throw e;
      }
    }
    return id;
  }

  private static @Nullable EconomyProvider resolve() {
    if (Bukkit.getPluginManager() == null
        || !Bukkit.getPluginManager().isPluginEnabled(MINT2_PLUGIN)
        || Bukkit.getServicesManager() == null) {
      return null;
    }
    RegisteredServiceProvider<EconomyProvider> registration =
        Bukkit.getServicesManager().getRegistration(EconomyProvider.class);
    return registration == null ? null : registration.getProvider();
  }
}
