package dev.mintychochip.databag.condition;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed armor-trim component from an item subject. Both keys are nullable
 * because vanilla trims always define both, but plugin-created metadata may
 * omit either side.
 *
 * @param material trim material id captured from the item, if any
 * @param pattern trim pattern id captured from the item, if any
 */
public record TrimSnapshot(@Nullable Key material, @Nullable Key pattern) {
  /**
   * Returns whether this snapshot satisfies optional expected trim ids.
   *
   * @param material expected trim material, or {@code null} to ignore
   * @param pattern expected trim pattern, or {@code null} to ignore
   * @return {@code true} when every non-null snapshot side matches the expected id
   */
  public boolean matches(@Nullable Key material, @Nullable Key pattern) {
    if (this.material != null
        && (material == null || !keysEqual(this.material, material))) {
      return false;
    }
    if (this.pattern != null
        && (pattern == null || !keysEqual(this.pattern, pattern))) {
      return false;
    }
    return true;
  }

  private static boolean keysEqual(Key expected, Key actual) {
    return expected.equals(actual)
        || expected.asString().equalsIgnoreCase(actual.asString());
  }

  /**
   * Returns whether either trim material or pattern is defined.
   *
   * @return {@code true} when material or pattern is non-null
   */
  public boolean isPresent() {
    return material != null || pattern != null;
  }
}
