package dev.mintychochip.databag.condition;

/** Marker for an immutable condition-evaluation subject. */
public sealed interface SubjectSnapshot
    permits SubjectSnapshot.Absent, EntitySnapshot, PlayerSnapshot, BlockSnapshot, ItemSubject {

  /**
   * Returns the explicit-absence subject.
   *
   * @return {@link Absent#INSTANCE}
   */
  static SubjectSnapshot absent() {
    return Absent.INSTANCE;
  }

  /** Explicit absence; distinct from a missing evaluation root. */
  enum Absent implements SubjectSnapshot {
    /** Singleton absent subject. */
    INSTANCE
  }
}
