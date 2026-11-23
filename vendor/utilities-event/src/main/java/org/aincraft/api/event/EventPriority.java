package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

/**
 * Ordering for listeners. Lower ordinal runs first.
 *
 * <p>Mirrors the familiar Bukkit/Paper ordering so existing mental models transfer: LOWEST → LOW →
 * NORMAL → HIGH → HIGHEST → MONITOR.
 *
 * <ul>
 *   <li>{@code LOWEST} … {@code HIGHEST}: normal mutation phases.
 *   <li>{@code MONITOR}: read-only observation; should not mutate or cancel.
 * </ul>
 */
@AvailableSince("2.0.0")
public enum EventPriority {
  LOWEST,
  LOW,
  NORMAL,
  HIGH,
  HIGHEST,
  MONITOR
}
