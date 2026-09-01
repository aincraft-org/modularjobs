package dev.mintychochip.boost;

import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.common.boost.BoostDataDocument;
import dev.mintychochip.common.boost.BoostDataDocument.BoostDocument;
import dev.mintychochip.common.boost.BoostDataDocument.RuleDocument;
import dev.mintychochip.common.boost.BoostDataDocument.SourceDocument;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import dev.mintychochip.container.boost.RuledBoostSource;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.databag.condition.ConditionSerializer;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** JSON codec for {@link SerializableBoostData}. Conditions are serializer bytes on each rule. */
public final class BoostDataCodec {

  private final ConditionSerializer conditions;
  private final BoostFactory boosts;

  /** Boost data codec. */
  public BoostDataCodec(@NotNull ConditionSerializer conditions, @NotNull BoostFactory boosts) {
    this.conditions = conditions;
    this.boosts = boosts;
  }

  /** Encodes {@code data} as UTF-8 JSON bytes. */
  @Contract(pure = true)
  public @NotNull byte[] write(@NotNull SerializableBoostData data) {
    String kind;
    String slots = null;
    String duration = null;
    if (data instanceof PassiveBoostData passive) {
      kind = "passive";
      slots = Base64.getEncoder().encodeToString(passive.slotSet().toByteArray());
    } else if (data instanceof ConsumableBoostData consumable) {
      kind = "consumable";
      duration = consumable.duration().toString();
    } else {
      throw new IllegalArgumentException("Unknown boost data: " + data.getClass().getName());
    }
    return BoostDataDocument.toJson(
        new BoostDataDocument(kind, slots, duration, toSource(data.boostSource())));
  }

  /** Encodes {@code source} as a source-only UTF-8 JSON payload. */
  @Contract(pure = true)
  public @NotNull byte[] writeSource(@NotNull BoostSource source) {
    return BoostDataDocument.toJson(new BoostDataDocument("source", null, null, toSource(source)));
  }

  /** Decodes UTF-8 JSON bytes into {@link SerializableBoostData}. */
  @Contract(pure = true)
  public @NotNull SerializableBoostData read(@NotNull byte[] bytes) {
    BoostDataDocument document = BoostDataDocument.fromJson(bytes);
    BoostSource source = fromSource(document.source());
    if ("passive".equalsIgnoreCase(document.kind())) {
      BitSet slotSet = new BitSet();
      if (document.slots() != null && !document.slots().isBlank()) {
        slotSet = BitSet.valueOf(Base64.getDecoder().decode(document.slots()));
      }
      return new PassiveBoostData(source, slotSet);
    }
    if (!"consumable".equalsIgnoreCase(document.kind())) {
      throw new IllegalArgumentException("Unknown boost data kind: " + document.kind());
    }
    Duration duration =
        document.duration() == null || document.duration().isBlank()
            ? Duration.ZERO
            : Duration.parse(document.duration());
    return new ConsumableBoostData(source, duration);
  }

  /** Decodes a source-only payload or a previously persisted boost-data payload. */
  @Contract(pure = true)
  public @NotNull BoostSource readSource(@NotNull byte[] bytes) {
    BoostDataDocument document = BoostDataDocument.fromJson(bytes);
    String kind = document.kind();
    if (!"source".equalsIgnoreCase(kind)
        && !"passive".equalsIgnoreCase(kind)
        && !"consumable".equalsIgnoreCase(kind)) {
      throw new IllegalArgumentException("Unknown boost source data kind: " + kind);
    }
    return fromSource(document.source());
  }

  @Contract(pure = true)
  private @NotNull SourceDocument toSource(@NotNull BoostSource source) {
    if (!(source instanceof RuledBoostSource ruled)) {
      throw new IllegalArgumentException(
          "Cannot serialize boost source type: " + source.getClass().getName());
    }
    List<RuleDocument> rules = new ArrayList<>();
    for (Rule rule : ruled.rules()) {
      byte[] conditionBytes = conditions.write(SnapshotCondition.unwrap(rule.condition()));
      rules.add(RuleDocument.of(rule.priority(), conditionBytes, toBoost(rule.boost())));
    }
    String key = source.key() != null ? source.key().asString() : "modularjobs:unknown";
    return new SourceDocument(key, source.description(), rules);
  }

  @Contract(pure = true)
  private @NotNull BoostSource fromSource(@NotNull SourceDocument document) {
    List<Rule> rules = new ArrayList<>();
    if (document.rules() != null) {
      for (RuleDocument rule : document.rules()) {
        dev.mintychochip.container.boost.Condition condition =
            SnapshotCondition.wrap(conditions.read(rule.conditionBytes()));
        rules.add(new Rule(condition, rule.priority(), fromBoost(rule.boost())));
      }
    }
    return new RuledBoostSourceImpl(rules, Key.key(document.key()), document.description());
  }

  @Contract(pure = true)
  private static @NotNull BoostDocument toBoost(@NotNull Boost boost) {
    return switch (boost) {
      case MultiplicativeBoostImpl mult ->
          new BoostDocument("multiplicative", mult.amount().doubleValue());
      case AdditiveBoostImpl add -> new BoostDocument("additive", add.amount().doubleValue());
      default ->
          throw new IllegalArgumentException(
              "Cannot serialize boost type: " + boost.getClass().getName());
    };
  }

  @Contract(pure = true)
  private @NotNull Boost fromBoost(@NotNull BoostDocument document) {
    BigDecimal amount = BigDecimal.valueOf(document.amount());
    return switch (document.type().toLowerCase()) {
      case "multiplicative" -> boosts.multiplicative(amount);
      case "additive" -> boosts.additive(amount);
      default -> throw new IllegalArgumentException("Unknown boost type: " + document.type());
    };
  }
}
