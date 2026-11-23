package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Set;

/**
 * True when the snapshot's current job key is one of {@code jobKeys}.
 *
 * @param jobKeys the job keys to match, with or without namespace
 */
public record JobCondition(Set<String> jobKeys) implements Condition {

  /**
   * Copies the configured job keys defensively.
   */
  public JobCondition {
    jobKeys = Set.copyOf(jobKeys);
  }

  @Override
  public boolean test(ConditionContext context) {
    if (!context.present() || context.jobKeys().isEmpty()) {
      return false;
    }
    for (String have : context.jobKeys()) {
      if (matches(have)) {
        return true;
      }
    }
    return false;
  }

  private boolean matches(String have) {
    for (String want : jobKeys) {
      if (have.equals(want) || have.equals(namespace(want)) || namespace(have).equals(want)) {
        return true;
      }
    }
    return false;
  }

  private static String namespace(String key) {
    return key.contains(":") ? key : "modularjobs:" + key;
  }
}
