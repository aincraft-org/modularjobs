package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches the entity's scoreboard team name against {@code expected}.
 * Fails closed when no living entity is present or the team is unset.
 *
 * @param expected required scoreboard team name
 */
public record TeamCondition(String expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    String actual = context.team();
    if (actual == null) {
      return false;
    }
    return actual.equals(expected);
  }
}
