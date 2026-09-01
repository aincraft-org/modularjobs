package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.Currency;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.JobTreeRecord;
import dev.mintychochip.domain.model.PayableRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class PersistenceConvertersTest {

  @Test
  void payableCurrencyMetadataSurvivesRecordRoundTrip() {
    PayableType type = payableType();
    Payable source =
        new Payable(
            type, PayableAmount.create(new BigDecimal("12.50"), Currency.of("TOKENS", "✦")));

    PayableRecord record = PersistenceConverters.toRecord(source);
    Payable restored = PersistenceConverters.fromRecord(record, ignored -> type);
    Currency currency = restored.amount().currency().orElseThrow();

    assertEquals("TOKENS", currency.identifier());
    assertEquals("✦", currency.symbol());
  }

  @Test
  void legacyCurrencyUsesIdentifierAsVisibleFallbackSymbol() {
    PayableType type = payableType();
    PayableRecord legacy =
        new PayableRecord(type.key().asString(), new BigDecimal("7.00"), "LEGACY_TOKENS", null);

    Payable restored = PersistenceConverters.fromRecord(legacy, ignored -> type);
    Currency currency = restored.amount().currency().orElseThrow();

    assertEquals("LEGACY_TOKENS", currency.identifier());
    assertEquals("LEGACY_TOKENS", currency.symbol());
  }

  @Test
  void currencylessPayableRemainsCurrencyless() {
    PayableType type = payableType();
    PayableRecord currencyless =
        new PayableRecord(type.key().asString(), BigDecimal.ONE, null, null);

    Payable restored = PersistenceConverters.fromRecord(currencyless, ignored -> type);

    assertTrue(restored.amount().currency().isEmpty());
  }

  @Test
  void convertsAnyValidPublicPlayerStateImplementation() {
    UUID playerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    JobKey jobKey = new JobKey(Key.key("modularjobs", "miner"));
    JobNodeKey nodeKey = new JobNodeKey(jobKey.key());
    JobNode root =
        new JobNodeImpl(jobKey, nodeKey, null, Component.text("Miner"), Component.text("Mines"));
    JobRecord source =
        new JobRecord(jobKey.asString(), "Miner", "Mines", 100, "level * 100", Map.of(), null);
    Job job =
        new JobImpl(
            jobKey,
            root,
            Map.of(nodeKey, root),
            new JobTreeRecord(source, Map.of(source.jobKey(), source)),
            100,
            parameters -> BigDecimal.valueOf(parameters.level() * 100L),
            Map.of());
    PlayerJobState state =
        new PlayerJobState() {
          @Override
          public @NotNull Job job() {
            return job;
          }

          @Override
          public @NotNull JobNode currentNode() {
            return root;
          }

          @Override
          public @NotNull UUID playerId() {
            return playerId;
          }

          @Override
          public @NotNull BigDecimal experience() {
            return new BigDecimal("450");
          }

          @Override
          public int level() {
            return 4;
          }

          @Override
          public @NotNull BigDecimal experienceForLevel(int level) {
            return BigDecimal.valueOf(level * 100L);
          }

          @Override
          public @NotNull PlayerJobState withExperience(@NotNull BigDecimal experience) {
            throw new UnsupportedOperationException();
          }

          @Override
          public @NotNull PlayerJobState withCurrentNode(@NotNull JobNodeKey key) {
            throw new UnsupportedOperationException();
          }
        };

    PlayerJobStateRecord record = PersistenceConverters.toRecord(state);

    assertEquals(playerId.toString(), record.playerId());
    assertEquals(jobKey.asString(), record.jobKey());
    assertEquals(nodeKey.asString(), record.currentNodeKey());
    assertEquals(new BigDecimal("450"), record.experience());
  }

  private static @NotNull PayableType payableType() {
    return new PayableType() {
      @Override
      public @NotNull PayableHandler handler() {
        return ignored -> {};
      }

      @Override
      public @NotNull Key key() {
        return Key.key("modularjobs", "economy");
      }
    };
  }
}
