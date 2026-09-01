package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Results;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.economy.AccountId;
import dev.mintychochip.economy.AccountService;
import dev.mintychochip.economy.AccountServiceImpl;
import dev.mintychochip.economy.Actor;
import dev.mintychochip.economy.ActorSource;
import dev.mintychochip.economy.ActorSources;
import dev.mintychochip.economy.Currency;
import dev.mintychochip.economy.CurrencyService;
import dev.mintychochip.economy.CurrencyServiceImpl;
import dev.mintychochip.economy.DefaultActorSources;
import dev.mintychochip.economy.EconomyProvider;
import dev.mintychochip.economy.PermissionBootstrap;
import dev.mintychochip.economy.PermissionService;
import dev.mintychochip.economy.PermissionTypes;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Drives {@link MintEconomyProvider} against an in-memory Mint2 economy (mint-common services)
 * registered on a mocked Bukkit services manager under the enabled {@code Mint2} plugin.
 */
class MintEconomyProviderTest {

  private UUID playerId;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    playerId = UUID.randomUUID();

    ActorSources actorSources = DefaultActorSources.create();
    ActorSource.Core core = actorSources.core();
    Actor console = core.console();
    PermissionService permissions = PermissionBootstrap.create(console);
    // Grant everything to the Vault system actor exactly like Mint2 does at enable.
    Results.take(
        permissions.editGlobal(
            core.vault(),
            current -> current.clear().add(PermissionTypes.DEPOSIT).add(PermissionTypes.WITHDRAW)));

    CurrencyService currencies = new CurrencyServiceImpl();
    Currency dollar = Results.take(currencies.createCurrency("dollar", '$'));
    Results.take(currencies.setDefault(dollar));

    AccountService accounts = new AccountServiceImpl(permissions, currencies);
    EconomyProvider mintProvider =
        new EconomyProvider() {

          @Override
          public String name() {
            return "mint";
          }

          @Override
          public int fractionalDigits() {
            return 2;
          }

          @Override
          public AccountService accountService() {
            return accounts;
          }

          @Override
          public CurrencyService currencyService() {
            return currencies;
          }

          @Override
          public PermissionService permissionService() {
            return permissions;
          }

          @Override
          public ActorSources actors() {
            return actorSources;
          }
        };
    Plugin mint2 = MockBukkit.createMockPlugin("Mint2");
    MockBukkit.getMock().getPluginManager().enablePlugin(mint2);
    MockBukkit.getMock()
        .getServicesManager()
        .register(EconomyProvider.class, mintProvider, mint2, ServicePriority.Lowest);
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void currencySupportFindsRegisteredMint2Service() {
    assertTrue(new MintEconomyProvider().isCurrencySupported());
  }

  @Test
  void depositMintsIntoPlayerAccount() {
    MintEconomyProvider provider = new MintEconomyProvider();

    boolean ok = provider.deposit(playerId, PayableAmount.create(new BigDecimal("42.50")));

    assertTrue(ok);
    assertBalance(playerId, "42.50");
  }

  @Test
  void depositRejectsNonPositiveAmountsWithoutTouchingMint() {
    MintEconomyProvider provider = new MintEconomyProvider();

    assertFalse(provider.deposit(playerId, PayableAmount.create(BigDecimal.ZERO)));
    assertFalse(provider.deposit(playerId, PayableAmount.create(new BigDecimal("-5"))));
  }

  private void assertBalance(UUID id, String expected) {
    RegisteredServiceProvider<EconomyProvider> registration =
        MockBukkit.getMock().getServicesManager().getRegistration(EconomyProvider.class);
    EconomyProvider mint = registration.getProvider();
    AccountService accounts = mint.accountService();
    Currency currency = Results.take(mint.currencyService().getDefault());
    AccountId account = new AccountId(id, AccountId.Type.PLAYER);
    BigDecimal actual = Results.take(accounts.balanceOf(account, currency)).amount().decimal();
    assertEquals(0, actual.compareTo(new BigDecimal(expected)));
  }
}
