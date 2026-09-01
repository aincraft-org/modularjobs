package dev.mintychochip.boost;

import java.util.BitSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Utility for parsing slot set specifications. */
public final class SlotSetParser {

  private SlotSetParser() {}

  /**
   * Parse a slot specification string into a BitSet.
   *
   * <p>Supported formats: - "all" - all slots (0-40) - "mainhand" - main hand slot (36) - "offhand"
   * - off hand slot (40) - "helmet" - helmet slot (39) - "chestplate" - chestplate slot (38) -
   * "leggings" - leggings slot (37) - "boots" - boots slot (36) - "armor" - all armor slots (36-39)
   * - "hotbar" - hotbar slots (0-8) - "inventory" - main inventory slots (9-35) - "5" - single slot
   * - "0-8" - range of slots - "0,9,36" - comma-separated slots
   *
   * @param spec the slot specification
   * @return the parsed BitSet
   * @throws IllegalArgumentException if the specification is invalid
   */
  @Contract(value = "null -> fail", pure = true)
  public static @NotNull BitSet parse(@NotNull String spec) {
    if (spec == null || spec.isBlank()) {
      throw new IllegalArgumentException("Slot specification cannot be null or blank");
    }

    BitSet bitSet = new BitSet(64);
    String trimmed = spec.trim().toLowerCase();

    switch (trimmed) {
      case "all" -> setRange(bitSet, 0, 40);
      case "mainhand" -> setRange(bitSet, 0, 8); // Mainhand can be any hotbar slot
      case "offhand" -> bitSet.set(40);
      case "helmet" -> bitSet.set(39);
      case "chestplate" -> bitSet.set(38);
      case "leggings" -> bitSet.set(37);
      case "boots" -> bitSet.set(36);
      case "armor" -> setRange(bitSet, 36, 39);
      case "hotbar" -> setRange(bitSet, 0, 8);
      case "inventory" -> setRange(bitSet, 9, 35);
      default -> parseCustom(bitSet, trimmed);
    }

    return bitSet;
  }

  private static void parseCustom(@NotNull BitSet bitSet, @NotNull String spec) {
    if (spec.contains(",")) {
      // Comma-separated slots: "0,9,36"
      String[] parts = spec.split(",");
      for (String part : parts) {
        String trimmedPart = part.trim();
        if (trimmedPart.contains("-")) {
          parseRange(bitSet, trimmedPart);
        } else {
          bitSet.set(parseSlot(trimmedPart));
        }
      }
    } else if (spec.contains("-")) {
      // Range: "0-8"
      parseRange(bitSet, spec);
    } else {
      // Single slot: "5"
      bitSet.set(parseSlot(spec));
    }
  }

  private static void parseRange(@NotNull BitSet bitSet, @NotNull String range) {
    String[] parts = range.split("-");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid range format: " + range);
    }
    int start = parseSlot(parts[0].trim());
    int end = parseSlot(parts[1].trim());
    if (start > end) {
      throw new IllegalArgumentException("Invalid range: start > end");
    }
    setRange(bitSet, start, end);
  }

  @Contract(pure = true)
  private static int parseSlot(@NotNull String slot) {
    try {
      int slotNum = Integer.parseInt(slot);
      if (slotNum < 0 || slotNum > 40) {
        throw new IllegalArgumentException("Slot must be between 0 and 40: " + slotNum);
      }
      return slotNum;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid slot number: " + slot, e);
    }
  }

  private static void setRange(@NotNull BitSet bitSet, int start, int end) {
    for (int i = start; i <= end; i++) {
      bitSet.set(i);
    }
  }
}
