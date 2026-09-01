package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.test.MockBukkitSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VaultEconomyProviderTest {

  private RecordingEconomy vault;
  private VaultEconomyProvider provider;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    vault = new RecordingEconomy();
    provider = new VaultEconomyProvider(vault.service());
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void depositPaysThePlayersVaultAccount() {
    UUID playerId = UUID.randomUUID();

    assertTrue(provider.deposit(playerId, PayableAmount.create(new BigDecimal("42.50"))));
    assertEquals(playerId, vault.player.getUniqueId());
    assertEquals(42.5D, vault.amount);
  }

  @Test
  void depositRejectsNullPayableAmount() {
    assertFalse(provider.deposit(UUID.randomUUID(), null));
    assertEquals(0, vault.depositCount);
  }

  @Test
  void depositReturnsFalseWhenVaultRejectsTheTransaction() {
    vault.response = new EconomyResponse(8.0D, 10.0D, ResponseType.FAILURE, "declined");

    assertFalse(provider.deposit(UUID.randomUUID(), PayableAmount.create(new BigDecimal("8.00"))));
  }

  @Test
  void depositRejectsAmountsVaultCannotSafelyRepresent() {
    UUID playerId = UUID.randomUUID();
    BigDecimal overflow = BigDecimal.valueOf(Double.MAX_VALUE).multiply(BigDecimal.TEN);

    assertFalse(provider.deposit(playerId, PayableAmount.create(BigDecimal.ZERO)));
    assertFalse(provider.deposit(playerId, PayableAmount.create(BigDecimal.ONE.negate())));
    assertFalse(provider.deposit(playerId, PayableAmount.create(overflow)));
    assertFalse(
        provider.deposit(playerId, PayableAmount.create(new BigDecimal("9007199254740993"))));
    assertFalse(provider.deposit(playerId, PayableAmount.create(new BigDecimal("1E-400"))));
    assertEquals(0, vault.depositCount);
  }

  @Test
  void currencySupportTracksTheVaultEconomyState() {
    assertTrue(provider.isCurrencySupported());

    vault.enabled = false;

    assertFalse(provider.isCurrencySupported());
  }

  @Test
  void depositReturnsFalseWhenVaultThrows() {
    vault.failure = new IllegalStateException("economy unavailable");

    assertFalse(provider.deposit(UUID.randomUUID(), PayableAmount.create(BigDecimal.ONE)));
  }

  private static final class RecordingEconomy {
    private boolean enabled = true;
    private EconomyResponse response = new EconomyResponse(42.5D, 42.5D, ResponseType.SUCCESS, "");
    private IllegalStateException failure;
    private OfflinePlayer player;
    private double amount;
    private int depositCount;

    private Economy service() {
      return (Economy)
          Proxy.newProxyInstance(
              Thread.currentThread().getContextClassLoader(),
              new Class<?>[] {Economy.class},
              this::invoke);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private Object invoke(Object proxy, Method method, Object[] arguments) {
      return switch (method.getName()) {
        case "isEnabled" -> enabled;
        case "depositPlayer" -> deposit(arguments);
        case "toString" -> "RecordingEconomy";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.toString());
      };
    }

    private EconomyResponse deposit(Object[] arguments) {
      depositCount++;
      player = (OfflinePlayer) arguments[0];
      amount = (double) arguments[1];
      if (failure != null) {
        throw failure;
      }
      return response;
    }
  }
}
