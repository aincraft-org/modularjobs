package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.container.Boost;
import dev.mintychochip.container.Currency;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.event.EventBus;
import dev.mintychochip.profession.ProfessionCategory;
import dev.mintychochip.profession.ProfessionDefinition;
import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.registry.RegistryKey;
import java.math.BigDecimal;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/** Consumer fixture compiled and executed on the API module's implementation-free classpath. */
class ApiOnlyConsumerTest {

  @Test
  void contractsAndValueFactoriesWorkWithoutCommonOrPaper() {
    Currency currency = Currency.of("TOKENS", "T");
    PayableAmount amount = PayableAmount.create(new BigDecimal("12.50"), currency);
    RegistryKey<Job> registryKey = RegistryKey.key(Key.key("modularjobs", "consumer_jobs"));
    ProfessionDefinition profession =
        new ProfessionDefinition("smithing", "smith", ProfessionCategory.CRAFTING, "Smithing");
    RecipeDefinition recipe =
        new RecipeDefinition(
            Key.key("modularjobs", "steel_sword"),
            profession.id(),
            3,
            2,
            Key.key("minecraft", "iron_sword"));

    assertEquals("TOKENS", amount.currency().orElseThrow().identifier());
    assertEquals("modularjobs:consumer_jobs", registryKey.key().asString());
    assertEquals("minecraft:iron_sword", recipe.craftOutputKey().asString());

    int[] events = {0};
    EventBus eventBus = new ApiOnlyEventBus();
    eventBus.subscribe(event -> events[0]++);
    assertEquals("published", eventBus.publish("published"));
    assertEquals(1, events[0]);

    Condition condition = context -> true;
    Boost boost = value -> value.add(BigDecimal.ONE);
    assertTrue(condition instanceof Condition);
    assertEquals(new BigDecimal("2"), boost.boost(BigDecimal.ONE));
    ClassLoader apiConsumerLoader = ApiOnlyConsumerTest.class.getClassLoader();
    assertNull(apiConsumerLoader.getResource("dev/mintychochip/common/event/EventBusImpl.class"));
    assertNull(apiConsumerLoader.getResource("dev/mintychochip/PluginContext.class"));
  }

  @Test
  void currencyFactoryExposesBothFieldsAndRejectsNulls() {
    Currency currency = Currency.of("TOKENS", "T");

    assertEquals("TOKENS", currency.identifier());
    assertEquals("T", currency.symbol());
    assertThrows(NullPointerException.class, () -> Currency.of(null, "T"));
    assertThrows(NullPointerException.class, () -> Currency.of("TOKENS", null));
  }

  @Test
  void jobAndNodeKeysAreDistinctTypedIdentities() {
    Key key = Key.key("modularjobs", "miner");
    JobKey jobKey = new JobKey(key);
    JobNodeKey nodeKey = new JobNodeKey(key);

    assertEquals(key, jobKey.key());
    assertEquals(key, nodeKey.key());
    assertNotEquals(jobKey, nodeKey);
  }

  private static final class ApiOnlyEventBus implements EventBus {
    private Consumer<Object> listener = ignored -> {};

    @Override
    public void subscribe(@NotNull Consumer<Object> listener) {
      this.listener = listener;
    }

    @Override
    public <T> @NotNull T publish(@NotNull T event) {
      listener.accept(event);
      return event;
    }
  }
}
