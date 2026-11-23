package dev.mintychochip.databag.condition;

import dev.mintychochip.databag.condition.builtin.AllOfCondition;
import dev.mintychochip.databag.condition.builtin.AnyOfCondition;
import dev.mintychochip.databag.condition.builtin.InvertedCondition;

/**
 * Pure predicate over a {@link ConditionContext}. Implementations must not look
 * up Bukkit state. The snapshot may describe a player, a living entity, a
 * block, or several of those at once.
 */
@FunctionalInterface
public interface Condition {

  /**
   * Returns {@code true} when this predicate holds for {@code context}.
   *
   * @param context immutable evaluation snapshot; implementations fail closed
   *     when the fields they need are absent
   * @return {@code true} when the predicate holds
   */
  boolean test(ConditionContext context);

  /**
   * Evaluates this predicate against a subject-graph {@link EvaluationContext}
   * by projecting it to a {@link ConditionContext}.
   *
   * @param evaluation root, graph, location, and loot inputs
   * @return {@code true} when the predicate holds
   */
  default boolean test(EvaluationContext evaluation) {
    return test(ConditionContexts.from(evaluation));
  }

  /**
   * Returns a condition that holds when both this predicate and {@code other} hold.
   *
   * @param other additional predicate
   * @return combined {@link AllOfCondition}
   */
  default Condition and(Condition other) {
    return Conditions.allOf(this, other);
  }

  /**
   * Returns a condition that holds when either this predicate or {@code other} holds.
   *
   * @param other alternative predicate
   * @return combined {@link AnyOfCondition}
   */
  default Condition or(Condition other) {
    return Conditions.anyOf(this, other);
  }

  /**
   * Returns a condition that holds when this predicate does not hold.
   *
   * @return inverted {@link InvertedCondition}
   */
  default Condition negate() {
    return Conditions.inverted(this);
  }
}
