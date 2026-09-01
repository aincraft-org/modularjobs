package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.Currency;
import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableHandler.PayableContext;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.test.MockBukkitSupport;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Drives shipped {@link EconomyProviderFactory} and {@link PayableWiring#economyHandlerFor}
 * selection and payment behavior without live provider plugins.
 */
class EconomyProviderFactoryTest {

  private Plugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void tryCreateReturnsNullWithoutMint() {
    assertNull(EconomyProviderFactory.tryCreate(plugin));
  }

  @Test
  void tryCreateSelectsMint2BeforeItsServiceRegisters() {
    Plugin mint2 = MockBukkit.createMockPlugin("Mint2");
    MockBukkit.getMock().getPluginManager().enablePlugin(mint2);

    EconomyProvider provider = EconomyProviderFactory.tryCreate(plugin);
    assertInstanceOf(MintEconomyProvider.class, provider);
    assertFalse(provider.isCurrencySupported());
  }

  @Test
  void tryCreateSelectsVaultWhenMint2IsAbsent() {
    registerVaultEconomy();

    assertInstanceOf(VaultEconomyProvider.class, EconomyProviderFactory.tryCreate(plugin));
  }

  @Test
  void tryCreatePrefersMint2WhenVaultIsAlsoAvailable() {
    registerVaultEconomy();
    Plugin mint2 = MockBukkit.createMockPlugin("Mint2");
    MockBukkit.getMock().getPluginManager().enablePlugin(mint2);

    assertInstanceOf(MintEconomyProvider.class, EconomyProviderFactory.tryCreate(plugin));
  }

  @Test
  void createOrFailUsesBlackholeByDefault() {
    EconomyProvider provider = EconomyProviderFactory.createOrFail(plugin);

    assertInstanceOf(BlackholeEconomyProvider.class, provider);
    assertTrue(provider.isCurrencySupported());
  }

  @Test
  void createOrFailThrowsWhenExplicitFailPolicyHasNoProvider() {
    plugin.getConfig().set("economy.missing-provider", "fail");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, () -> EconomyProviderFactory.createOrFail(plugin));

    assertTrue(ex.getMessage().contains("economy.missing-provider"));
  }

  @Test
  void legacyRequiredTrueStillFailsWithoutProvider() {
    plugin.getConfig().set("economy.missing-provider", null);
    plugin.getConfig().set("economy.required", true);

    assertThrows(IllegalStateException.class, () -> EconomyProviderFactory.createOrFail(plugin));
  }

  @Test
  void explicitBlackholePolicyOverridesLegacyRequiredFlag() {
    plugin.getConfig().set("economy.required", true);
    plugin.getConfig().set("economy.missing-provider", "BLACKHOLE");

    assertInstanceOf(BlackholeEconomyProvider.class, EconomyProviderFactory.createOrFail(plugin));
  }

  @Test
  void unknownMissingProviderPolicyFailsConfiguration() {
    plugin.getConfig().set("economy.missing-provider", "unknown");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> EconomyProviderFactory.createOrFail(plugin));

    assertTrue(ex.getMessage().contains("economy.missing-provider"));
  }

  @Test
  void blackholeAcceptsPositiveAndRejectsNonPositiveAmounts() {
    EconomyProvider provider = new BlackholeEconomyProvider(plugin);

    assertTrue(provider.deposit(UUID.randomUUID(), PayableAmount.create(BigDecimal.ONE)));
    assertFalse(provider.deposit(UUID.randomUUID(), PayableAmount.create(BigDecimal.ZERO)));
    assertFalse(provider.deposit(UUID.randomUUID(), PayableAmount.create(BigDecimal.ONE.negate())));
  }

  @Test
  void economyHandlerDelegatesToProvider() {
    boolean[] deposited = {false};
    EconomyProvider provider =
        new EconomyProvider() {
          @Override
          public boolean isCurrencySupported() {
            return false;
          }

          @Override
          public boolean deposit(@NotNull UUID playerId, @NotNull PayableAmount payableAmount) {
            deposited[0] = true;
            return true;
          }
        };

    PayableHandler handler = PayableWiring.economyHandlerFor(provider);
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    assertDoesNotThrow(() -> handler.pay(contextFor(handler, player)));
    assertTrue(deposited[0], "shipped handler must call EconomyProvider.deposit");
  }

  @Test
  void requiredDefaultsFalse() {
    Plugin fresh = MockBukkit.createMockPlugin("JobsFresh");

    assertFalse(EconomyProviderFactory.isRequired(fresh));
  }

  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  private void registerVaultEconomy() {
    Plugin vault = MockBukkit.createMockPlugin("Vault");
    MockBukkit.getMock().getPluginManager().enablePlugin(vault);
    Economy economy =
        (Economy)
            Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {Economy.class},
                (proxy, method, arguments) ->
                    switch (method.getName()) {
                      case "isEnabled" -> true;
                      case "toString" -> "FactoryVaultEconomy";
                      case "hashCode" -> System.identityHashCode(proxy);
                      case "equals" -> proxy == arguments[0];
                      default -> throw new UnsupportedOperationException(method.toString());
                    });
    MockBukkit.getMock()
        .getServicesManager()
        .register(Economy.class, economy, vault, ServicePriority.Normal);
  }

  private static @NotNull PayableContext contextFor(
      @NotNull PayableHandler handler, @NotNull OfflinePlayer player) {
    PayableType type =
        new PayableType() {
          @Override
          public @NotNull PayableHandler handler() {
            return handler;
          }

          @Override
          public @NotNull Key key() {
            return Key.key("modularjobs", "economy");
          }
        };
    Payable payable = new Payable(type, PayableAmount.create(BigDecimal.TEN, Currency.USD));
    PlayerJobState state =
        (PlayerJobState)
            Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {PlayerJobState.class},
                (proxy, method, arguments) -> {
                  throw new UnsupportedOperationException(method.toString());
                });
    return new PayableContext(player.getUniqueId(), payable, state);
  }
}
