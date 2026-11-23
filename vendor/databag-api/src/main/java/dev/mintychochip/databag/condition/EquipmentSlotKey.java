package dev.mintychochip.databag.condition;

import java.util.Locale;

/** Equipment slots supported by entity equipment predicates. */
public enum EquipmentSlotKey {
  /** Primary hand equipment slot. */
  MAINHAND,
  /** Off-hand equipment slot. */
  OFFHAND,
  /** Head armor slot. */
  HEAD,
  /** Chest armor slot. */
  CHEST,
  /** Leg armor slot. */
  LEGS,
  /** Foot armor slot. */
  FEET;

  /**
   * Vanilla JSON equipment slot name.
   *
   * @return lowercase enum name
   */
  public String jsonName() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * Parses a vanilla or alias equipment slot name.
   *
   * @param raw JSON slot name, for example {@code mainhand} or {@code helmet}
   * @return matching slot key
   * @throws IllegalArgumentException when {@code raw} is unknown
   */
  public static EquipmentSlotKey fromJson(String raw) {
    return switch (raw.toLowerCase(Locale.ROOT)) {
      case "mainhand", "main_hand" -> MAINHAND;
      case "offhand", "off_hand" -> OFFHAND;
      case "head", "helmet" -> HEAD;
      case "chest", "chestplate" -> CHEST;
      case "legs", "leggings" -> LEGS;
      case "feet", "boots" -> FEET;
      default -> throw new IllegalArgumentException("Unknown equipment slot: " + raw);
    };
  }
}
