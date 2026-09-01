package dev.mintychochip.editor;

import java.time.Instant;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-local handoff for a REST editor session.
 *
 * @param sessionCode public REST session identifier
 * @param token secret REST session token
 * @param playerId player who created and may apply the session
 * @param createdAt local creation time
 * @param expiresAt REST session expiry time
 */
public record EditorSession(
    @NotNull String sessionCode,
    @NotNull String token,
    @NotNull UUID playerId,
    @NotNull Instant createdAt,
    @NotNull Instant expiresAt) {
  /** Returns whether expired. */
  @Contract(pure = true)
  public boolean isExpired(@NotNull Instant now) {
    return !expiresAt.isAfter(now);
  }
}
