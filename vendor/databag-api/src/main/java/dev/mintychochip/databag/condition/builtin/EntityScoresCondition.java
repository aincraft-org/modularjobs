package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Matches scoreboard objective values against optional min/max bounds per objective.
 *
 * @param scores objective name to inclusive bounds map; copied defensively
 */
public record EntityScoresCondition(Map<String, Bound> scores) implements Condition {

  /**
   * Copies the objective bounds map defensively.
   */
  public EntityScoresCondition {
    scores = Map.copyOf(Objects.requireNonNull(scores, "scores"));
    if (scores.isEmpty()) {
      throw new IllegalArgumentException("entity scores must not be empty");
    }
  }

  /**
   * Optional inclusive bounds for one scoreboard objective.
   *
   * @param min inclusive minimum score, or {@code null} for no lower bound
   * @param max inclusive maximum score, or {@code null} for no upper bound
   */
  public record Bound(@Nullable Integer min, @Nullable Integer max) {

    /**
     * Requires at least one inclusive score bound.
     */
    public Bound {
      if (min == null && max == null) {
        throw new IllegalArgumentException("at least one bound must be non-null");
      }
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    for (Map.Entry<String, Bound> entry : scores.entrySet()) {
      Integer actual = context.scores().get(entry.getKey());
      if (actual == null) {
        return false;
      }
      Bound bound = entry.getValue();
      if (bound.min() != null && actual < bound.min()) {
        return false;
      }
      if (bound.max() != null && actual > bound.max()) {
        return false;
      }
    }
    return true;
  }
}
