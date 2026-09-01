package dev.mintychochip.service;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Service for managing player preferences. Controls settings like entries per page for info
 * displays.
 */
public interface PreferencesService {

  /**
   * Gets the number of entries to display per page for the given player.
   *
   * @param playerId The player id to get the preference for
   * @return The number of entries per page (default: 10)
   */
  int getEntriesPerPage(@NotNull UUID playerId);

  /**
   * Sets the number of entries to display per page for the given player.
   *
   * @param playerId The player id to set the preference for
   * @param entries The number of entries per page
   */
  void setEntriesPerPage(@NotNull UUID playerId, int entries);

  /**
   * Gets the default entries per page for all players.
   *
   * @return The default number of entries per page
   */
  int getDefaultEntriesPerPage();

  /**
   * Gets whether the player prefers GUI mode over chat mode for info displays.
   *
   * @param playerId The player id to check
   * @return true if GUI mode is preferred, false for chat mode
   */
  boolean prefersGuiMode(@NotNull UUID playerId);

  /**
   * Sets whether the player prefers GUI mode over chat mode.
   *
   * @param playerId The player id to set the preference for
   * @param guiMode true for GUI mode, false for chat mode
   */
  void setGuiMode(@NotNull UUID playerId, boolean guiMode);
}
