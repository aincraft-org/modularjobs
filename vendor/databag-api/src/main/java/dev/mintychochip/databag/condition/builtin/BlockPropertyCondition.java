package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Objects;

/**
 * Matches one block-state property (for example {@code facing=north}).
 *
 * @param name block-state property name
 * @param value expected property value; compared case-insensitively
 */
public record BlockPropertyCondition(String name, String value) implements Condition {

  /** Validates that name and value are non-null and the name is non-blank. */
  public BlockPropertyCondition {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
    if (name.isBlank()) {
      throw new IllegalArgumentException("block property name must be non-blank");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.blockId() == null) {
      return false;
    }
    String actual = context.blockProperties().get(name);
    if (actual == null) {
      return false;
    }
    return actual.equalsIgnoreCase(value);
  }
}
