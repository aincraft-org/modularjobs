package dev.mintychochip.boost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.common.boost.BoostDataDocument;
import dev.mintychochip.common.boost.BoostDataDocument.SourceDocument;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import dev.mintychochip.container.boost.RuledBoostSource;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.databag.condition.Conditions;
import dev.mintychochip.databag.gson.GsonConditionSerializer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.BitSet;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class BoostDataCodecTest {

  private final BoostDataCodec codec =
      new BoostDataCodec(GsonConditionSerializer.gson(), BoostFactoryImpl.INSTANCE);

  @Test
  void passiveDataRoundTrips() {
    BitSet slots = new BitSet();
    slots.set(4);
    slots.set(12);

    SerializableBoostData decoded =
        codec.read(codec.write(new PassiveBoostData(sampleSource(), slots)));

    PassiveBoostData passive = assertInstanceOf(PassiveBoostData.class, decoded);
    assertEquals(slots, passive.slotSet());
    assertSourceRoundTrip(passive.boostSource());
  }

  @Test
  void consumableDataRoundTrips() {
    Duration duration = Duration.ofMinutes(37);

    SerializableBoostData decoded =
        codec.read(codec.write(new ConsumableBoostData(sampleSource(), duration)));

    ConsumableBoostData consumable = assertInstanceOf(ConsumableBoostData.class, decoded);
    assertEquals(duration, consumable.duration());
    assertSourceRoundTrip(consumable.boostSource());
  }

  @Test
  void nonRuledSourceIsRejected() {
    PassiveBoostData data = new PassiveBoostData(nonRuledSource(), new BitSet());

    assertThrows(IllegalArgumentException.class, () -> codec.write(data));
  }

  @Test
  void unsupportedBoostIsRejected() {
    Boost unsupported = amount -> amount;
    RuledBoostSourceImpl source =
        new RuledBoostSourceImpl(
            List.of(new Rule(SnapshotCondition.wrap(Conditions.always()), 7, unsupported)),
            Key.key("modularjobs", "unsupported_boost"),
            "unsupported boost source");

    assertThrows(
        IllegalArgumentException.class,
        () -> codec.write(new PassiveBoostData(source, new BitSet())));
  }

  @Test
  void unknownDataKindIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> codec.read(unknownKindPayload()));
  }

  @Test
  void unknownSourceKindIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> codec.readSource(unknownKindPayload()));
  }

  @Test
  void sourceRoundTripsOnlyThroughDedicatedCodecPath() {
    byte[] payload = codec.writeSource(sampleSource());

    assertSourceRoundTrip(codec.readSource(payload));
    assertThrows(IllegalArgumentException.class, () -> codec.read(payload));
  }

  @Test
  void sourceReaderAcceptsLegacyConsumablePayload() {
    byte[] payload =
        """
        {
          "kind": "consumable",
          "slots": null,
          "duration": "PT1H",
          "source": {
            "key": "modularjobs:legacy",
            "description": "legacy source",
            "rules": []
          }
        }
        """
            .getBytes(StandardCharsets.UTF_8);

    RuledBoostSource source = assertInstanceOf(RuledBoostSource.class, codec.readSource(payload));
    assertEquals(Key.key("modularjobs", "legacy"), source.key());
    assertEquals("legacy source", source.description());
    assertEquals(0, source.rules().size());
  }

  private static byte[] unknownKindPayload() {
    return BoostDataDocument.toJson(
        new BoostDataDocument(
            "future",
            null,
            null,
            new SourceDocument("modularjobs:unknown_kind", "unknown kind source", List.of())));
  }

  private static @NotNull BoostSource nonRuledSource() {
    return new BoostSource() {
      @Override
      public @NotNull List<Boost> evaluate(@NotNull BoostContext context) {
        return List.of();
      }

      @Override
      public @NotNull Key key() {
        return Key.key("modularjobs", "runtime_only");
      }

      @Override
      public @NotNull String description() {
        return "runtime-only source";
      }
    };
  }

  private static @NotNull RuledBoostSourceImpl sampleSource() {
    Rule rule =
        new Rule(
            SnapshotCondition.wrap(Conditions.always()),
            7,
            new MultiplicativeBoostImpl(new BigDecimal("1.25")));
    return new RuledBoostSourceImpl(
        List.of(rule), Key.key("modularjobs", "codec_test"), "codec test source");
  }

  private static void assertSourceRoundTrip(@NotNull BoostSource source) {
    RuledBoostSource ruled = assertInstanceOf(RuledBoostSource.class, source);
    assertEquals(Key.key("modularjobs", "codec_test"), ruled.key());
    assertEquals("codec test source", ruled.description());
    assertEquals(1, ruled.rules().size());

    Rule rule = ruled.rules().getFirst();
    assertEquals(7, rule.priority());
    assertInstanceOf(SnapshotCondition.class, rule.condition());
    assertEquals(
        0,
        new BigDecimal("10.00").compareTo(rule.boost().boost(new BigDecimal("8"))),
        "decoded 1.25x boost must preserve its behavior");
  }
}
