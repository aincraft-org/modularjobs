package dev.mintychochip.databag.condition;

/**
 * Safety bounds for target-path resolution.
 *
 * @param maxDepth maximum {@link TargetPath} segment count permitted during resolve
 */
public record SubjectGraphLimits(int maxDepth) {

  /** Default maximum path depth. */
  public static final int DEFAULT_MAX_DEPTH = 16;
  /** Limits using {@link #DEFAULT_MAX_DEPTH}. */
  public static final SubjectGraphLimits DEFAULT = new SubjectGraphLimits(DEFAULT_MAX_DEPTH);

  /** Rejects a negative depth. */
  public SubjectGraphLimits {
    if (maxDepth < 0) {
      throw new IllegalArgumentException("maxDepth must be >= 0");
    }
  }
}
