package dev.mintychochip.boost;

import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.databag.condition.builtin.AllOfCondition;
import dev.mintychochip.databag.condition.builtin.AlwaysCondition;
import dev.mintychochip.databag.condition.builtin.AnyOfCondition;
import dev.mintychochip.databag.condition.builtin.BabyCondition;
import dev.mintychochip.databag.condition.builtin.BiomeCondition;
import dev.mintychochip.databag.condition.builtin.BlockIdCondition;
import dev.mintychochip.databag.condition.builtin.BlockPropertyCondition;
import dev.mintychochip.databag.condition.builtin.EntityTypeCondition;
import dev.mintychochip.databag.condition.builtin.FluidCondition;
import dev.mintychochip.databag.condition.builtin.FlyingCondition;
import dev.mintychochip.databag.condition.builtin.GameModeCondition;
import dev.mintychochip.databag.condition.builtin.GlidingCondition;
import dev.mintychochip.databag.condition.builtin.InvertedCondition;
import dev.mintychochip.databag.condition.builtin.JobCondition;
import dev.mintychochip.databag.condition.builtin.OnFireCondition;
import dev.mintychochip.databag.condition.builtin.OnGroundCondition;
import dev.mintychochip.databag.condition.builtin.PlayerResourceCondition;
import dev.mintychochip.databag.condition.builtin.PotionAmplifierCondition;
import dev.mintychochip.databag.condition.builtin.PotionDurationCondition;
import dev.mintychochip.databag.condition.builtin.PotionPresentCondition;
import dev.mintychochip.databag.condition.builtin.SneakingCondition;
import dev.mintychochip.databag.condition.builtin.SprintingCondition;
import dev.mintychochip.databag.condition.builtin.SwimmingCondition;
import dev.mintychochip.databag.condition.builtin.WeatherCondition;
import dev.mintychochip.databag.condition.builtin.WorldCondition;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Formats a boost condition tree for admin command output. */
public final class ConditionTreeFormatter {

  private ConditionTreeFormatter() {}

  /** Format. */
  public static @NotNull List<String> format(@NotNull Condition condition, @NotNull String indent) {
    List<String> lines = new ArrayList<>();
    formatBoost(condition, indent, "", true, lines);
    return lines;
  }

  private static void formatBoost(
      @NotNull Condition condition,
      @NotNull String baseIndent,
      @NotNull String prefix,
      boolean isLast,
      @NotNull List<String> lines) {
    if (condition instanceof SnapshotCondition snapshot) {
      formatApi(snapshot.delegate(), baseIndent, prefix, isLast, lines);
      return;
    }
    String connector = isLast ? "└── " : "├── ";
    lines.add(
        baseIndent
            + prefix
            + connector
            + condition.getClass().getSimpleName().replace("Impl", "").replace("Condition", ""));
  }

  private static void formatApi(
      @NotNull dev.mintychochip.databag.condition.Condition condition,
      @NotNull String baseIndent,
      @NotNull String prefix,
      boolean isLast,
      @NotNull List<String> lines) {
    String connector = isLast ? "└── " : "├── ";
    String childPrefix = isLast ? "    " : "│   ";
    switch (condition) {
      case AlwaysCondition ignored -> lines.add(baseIndent + prefix + connector + "Always");
      case AllOfCondition all -> {
        lines.add(baseIndent + prefix + connector + "AND");
        for (int i = 0; i < all.terms().size(); i++) {
          formatApi(
              all.terms().get(i),
              baseIndent,
              prefix + childPrefix,
              i == all.terms().size() - 1,
              lines);
        }
      }
      case AnyOfCondition any -> {
        lines.add(baseIndent + prefix + connector + "OR");
        for (int i = 0; i < any.terms().size(); i++) {
          formatApi(
              any.terms().get(i),
              baseIndent,
              prefix + childPrefix,
              i == any.terms().size() - 1,
              lines);
        }
      }
      case InvertedCondition inverted -> {
        lines.add(baseIndent + prefix + connector + "NOT");
        formatApi(inverted.term(), baseIndent, prefix + childPrefix, true, lines);
      }
      case SneakingCondition sneak ->
          lines.add(baseIndent + prefix + connector + "Sneaking: " + sneak.expected());
      case SprintingCondition sprint ->
          lines.add(baseIndent + prefix + connector + "Sprinting: " + sprint.expected());
      case EntityTypeCondition type ->
          lines.add(baseIndent + prefix + connector + "Entity: " + type.entityType().asString());
      case OnFireCondition fire ->
          lines.add(baseIndent + prefix + connector + "On fire: " + fire.expected());
      case OnGroundCondition ground ->
          lines.add(baseIndent + prefix + connector + "On ground: " + ground.expected());
      case SwimmingCondition swim ->
          lines.add(baseIndent + prefix + connector + "Swimming: " + swim.expected());
      case BabyCondition baby ->
          lines.add(baseIndent + prefix + connector + "Baby: " + baby.expected());
      case GlidingCondition glide ->
          lines.add(baseIndent + prefix + connector + "Gliding: " + glide.expected());
      case FlyingCondition fly ->
          lines.add(baseIndent + prefix + connector + "Flying: " + fly.expected());
      case GameModeCondition mode ->
          lines.add(baseIndent + prefix + connector + "Game mode: " + mode.gameMode());
      case BlockIdCondition block ->
          lines.add(baseIndent + prefix + connector + "Block: " + block.blockId().asString());
      case BlockPropertyCondition prop ->
          lines.add(baseIndent + prefix + connector + "Block " + prop.name() + "=" + prop.value());
      case BiomeCondition biome ->
          lines.add(baseIndent + prefix + connector + "Biome: " + biome.biomeKey().value());
      case WorldCondition world ->
          lines.add(baseIndent + prefix + connector + "World: " + world.worldName());
      case WeatherCondition weather ->
          lines.add(baseIndent + prefix + connector + "Weather: " + weather.state());
      case FluidCondition fluid ->
          lines.add(baseIndent + prefix + connector + "In Liquid: " + fluid.fluidKey().value());
      case PlayerResourceCondition resource ->
          lines.add(
              baseIndent
                  + prefix
                  + connector
                  + resource.type()
                  + " "
                  + resource.operator()
                  + " "
                  + resource.expected());
      case PotionPresentCondition potion ->
          lines.add(baseIndent + prefix + connector + "Has Potion: " + potion.effectKey().value());
      case PotionAmplifierCondition potion ->
          lines.add(
              baseIndent
                  + prefix
                  + connector
                  + "Potion: "
                  + potion.effectKey().value()
                  + " amplifier "
                  + potion.operator()
                  + " "
                  + potion.expected());
      case PotionDurationCondition potion ->
          lines.add(
              baseIndent
                  + prefix
                  + connector
                  + "Potion: "
                  + potion.effectKey().value()
                  + " duration "
                  + potion.operator()
                  + " "
                  + potion.expected());
      case JobCondition job -> lines.add(baseIndent + prefix + connector + "Job: " + job.jobKeys());
      default -> lines.add(baseIndent + prefix + connector + condition.getClass().getSimpleName());
    }
  }
}
