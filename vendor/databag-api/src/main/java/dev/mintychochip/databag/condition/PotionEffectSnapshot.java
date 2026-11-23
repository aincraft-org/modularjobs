package dev.mintychochip.databag.condition;

/**
 * One active potion effect as seen by conditions.
 *
 * @param amplifier effect amplifier (0 = level I)
 * @param duration remaining duration in ticks
 */
public record PotionEffectSnapshot(int amplifier, int duration) {}
