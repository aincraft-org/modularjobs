package dev.mintychochip.databag.condition;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Result of resolving one target path without throwing for absent graph data.
 *
 * @param resolvedSubject the resolved subject, or {@code null}
 * @param resolvedLocation the resolved location, or {@code null}
 * @param failureReason why resolution failed, or {@code null} on success
 */
public record PathResolution(
    @Nullable SubjectSnapshot resolvedSubject,
    @Nullable LocationSnapshot resolvedLocation,
    @Nullable Failure failureReason) {

  /** Why a path failed to resolve. */
  public enum Failure {
    /** No graph edge for the requested relation. */
    MISSING_RELATION,
    /** No location on the subject or evaluation context. */
    MISSING_LOCATION,
    /** The equipment slot was empty or the subject was not an entity. */
    MISSING_EQUIPMENT,
    /** The path exceeded {@link SubjectGraphLimits#maxDepth()}. */
    MAX_DEPTH
  }

  /** Requires exactly one of subject, location, or failure. */
  public PathResolution {
    int populated = (resolvedSubject == null ? 0 : 1)
        + (resolvedLocation == null ? 0 : 1)
        + (failureReason == null ? 0 : 1);
    if (populated != 1) {
      throw new IllegalArgumentException(
          "exactly one of subject, location, or failure must be present");
    }
  }


  /**
   * @param value resolved subject
   * @return a successful subject resolution
   */
  public static PathResolution subject(SubjectSnapshot value) {
    return new PathResolution(value, null, null);
  }

  /**
   * @param value resolved location
   * @return a successful location resolution
   */
  public static PathResolution location(LocationSnapshot value) {
    return new PathResolution(null, value, null);
  }

  /**
   * @param value failure reason
   * @return a failed resolution
   */
  public static PathResolution failed(Failure value) {
    return new PathResolution(null, null, value);
  }

  /**
   * @return {@code true} when a subject or location was resolved
   */
  public boolean present() {
    return failureReason == null && (resolvedSubject != null || resolvedLocation != null);
  }

  /**
   * @return the resolved subject, if this result is a subject
   */
  public Optional<SubjectSnapshot> subject() {
    return Optional.ofNullable(resolvedSubject);
  }

  /**
   * @return the resolved location, if this result is a location
   */
  public Optional<LocationSnapshot> location() {
    return Optional.ofNullable(resolvedLocation);
  }

  /**
   * @return the failure reason, if resolution failed
   */
  public Optional<Failure> failure() {
    return Optional.ofNullable(failureReason);
  }
}
