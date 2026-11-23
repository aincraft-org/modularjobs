package dev.mintychochip.databag.condition;

import dev.mintychochip.databag.condition.builtin.EntityTargetCondition;

/**
 * Entity slot referenced by {@link EntityTargetCondition}.
 */
public enum EntityTarget {
  /** The entity described by the current {@link ConditionContext}. */
  THIS("this"),
  /** The entity that last damaged this entity. */
  ATTACKER("attacker"),
  /** The entity that directly dealt the most recent damage. */
  DIRECT_ATTACKER("direct_attacker"),
  /** The player that last attacked this entity. */
  ATTACKING_PLAYER("attacking_player"),
  /** The entity currently targeted by this entity. */
  TARGET_ENTITY("target_entity"),
  /** The entity this entity is interacting with. */
  INTERACTING_ENTITY("interacting_entity");

  private final String jsonName;

  EntityTarget(String jsonName) {
    this.jsonName = jsonName;
  }
  /**
   * Vanilla JSON slot name for this target.
   *
   * @return lowercase snake-case identifier
   */

  public String jsonName() {
    return jsonName;
  }
}
