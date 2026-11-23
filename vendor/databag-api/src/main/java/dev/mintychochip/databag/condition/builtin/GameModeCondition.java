package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Locale;
import java.util.Objects;

/**
 * Player-only game mode ({@code survival}, {@code creative}, {@code adventure},
 * {@code spectator}). Non-player living entities fail closed.
 *
 * @param gameMode expected game mode name, normalized to lowercase
 */
public record GameModeCondition(String gameMode) implements Condition {

  /**
   * Validates and normalizes the game mode name to lowercase.
   */
  public GameModeCondition {
    Objects.requireNonNull(gameMode, "gameMode");
    if (gameMode.isBlank()) {
      throw new IllegalArgumentException("gameMode must be non-blank");
    }
    gameMode = gameMode.toLowerCase(Locale.ROOT);
  }

  @Override
  public boolean test(ConditionContext context) {
    if (!context.present() || context.gameMode() == null) {
      return false;
    }
    return gameMode.equalsIgnoreCase(context.gameMode());
  }
}
