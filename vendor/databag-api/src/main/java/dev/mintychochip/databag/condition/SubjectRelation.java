package dev.mintychochip.databag.condition;

/** Named edge between immutable evaluation subjects. */
public enum SubjectRelation {
  /** The entity that last damaged this subject. */
  ATTACKER,
  /** The entity that directly dealt the most recent damage. */
  DIRECT_ATTACKER,
  /** The player that last attacked this subject. */
  ATTACKING_PLAYER,
  /** The entity currently targeted by this subject. */
  TARGET_ENTITY,
  /** The entity this subject is interacting with. */
  INTERACTING_ENTITY,
  /** The vehicle this subject is riding. */
  VEHICLE,
  /** A passenger riding this subject. */
  PASSENGER,
  /** The block this subject is standing on. */
  STANDING_ON,
  /** The item subject in scope. */
  ITEM
}
