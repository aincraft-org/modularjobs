package dev.mintychochip.repository;

import org.jetbrains.annotations.NotNull;

/** Signals a write-back persistence failure while preserving the original cause. */
public final class WriteBackException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Write back exception. */
  public WriteBackException(@NotNull String message, @NotNull Throwable cause) {
    super(message, cause);
  }
}
