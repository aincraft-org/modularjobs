package dev.mintychochip.databag.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable navigation from the evaluation root to a related subject or location. */
public final class TargetPath {

  private final List<Segment> segments;

  private TargetPath(List<Segment> segments) {
    this.segments = List.copyOf(segments);
  }

  /**
   * Returns a path that resolves to the evaluation root.
   *
   * @return the empty path
   */
  public static TargetPath root() {
    return new TargetPath(List.of());
  }

  /**
   * @return this path plus an attacker segment
   */
  public TargetPath attacker() {
    return relation(SubjectRelation.ATTACKER);
  }

  /**
   * @return this path plus a direct-attacker segment
   */
  public TargetPath directAttacker() {
    return relation(SubjectRelation.DIRECT_ATTACKER);
  }

  /**
   * @return this path plus an attacking-player segment
   */
  public TargetPath attackingPlayer() {
    return relation(SubjectRelation.ATTACKING_PLAYER);
  }

  /**
   * @return this path plus a target-entity segment
   */
  public TargetPath targetEntity() {
    return relation(SubjectRelation.TARGET_ENTITY);
  }

  /**
   * @return this path plus an interacting-entity segment
   */
  public TargetPath interactingEntity() {
    return relation(SubjectRelation.INTERACTING_ENTITY);
  }

  /**
   * @return this path plus a vehicle segment
   */
  public TargetPath vehicle() {
    return relation(SubjectRelation.VEHICLE);
  }

  /**
   * @return this path plus the first passenger segment
   */
  public TargetPath passenger() {
    return passenger(0);
  }

  /**
   * Appends a passenger segment at {@code index}.
   *
   * @param index passenger index, {@code >= 0}
   * @return a new path
   */
  public TargetPath passenger(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("passenger index must be >= 0");
    }
    return append(new RelationSegment(SubjectRelation.PASSENGER, index));
  }

  /**
   * @return this path plus a standing-on segment
   */
  public TargetPath standingOn() {
    return relation(SubjectRelation.STANDING_ON);
  }

  /**
   * @return this path plus an item segment
   */
  public TargetPath item() {
    return relation(SubjectRelation.ITEM);
  }

  /**
   * Appends an equipment-slot segment.
   *
   * @param slot equipment slot
   * @return a new path
   */
  public TargetPath equipment(EquipmentSlotKey slot) {
    return append(new EquipmentSegment(slot));
  }

  /**
   * Appends a terminal location segment.
   *
   * @return a new path
   */
  public TargetPath location() {
    return append(LocationSegment.INSTANCE);
  }

  /**
   * Resolves this path against {@code context} with default depth limits.
   *
   * @param context evaluation inputs
   * @return the resolution result
   */
  public PathResolution resolve(EvaluationContext context) {
    return resolve(context, SubjectGraphLimits.DEFAULT);
  }

  /**
   * Resolves this path against {@code context}.
   *
   * @param context evaluation inputs
   * @param limits depth limits
   * @return the resolution result
   */
  public PathResolution resolve(EvaluationContext context, SubjectGraphLimits limits) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(limits, "limits");
    if (segments.size() > limits.maxDepth()) {
      return PathResolution.failed(PathResolution.Failure.MAX_DEPTH);
    }
    SubjectSnapshot current = context.root();
    for (Segment segment : segments) {
      if (segment instanceof RelationSegment relation) {
        var next =
            context.graph().related(current, relation.relation(), relation.index());
        if (next.isEmpty()) {
          return PathResolution.failed(PathResolution.Failure.MISSING_RELATION);
        }
        current = next.get();
      } else if (segment instanceof EquipmentSegment equipment) {
        EntitySnapshot entity =
            current instanceof PlayerSnapshot player ? player.entity()
                : current instanceof EntitySnapshot value ? value : null;
        if (entity == null || !entity.equipment().containsKey(equipment.slot())) {
          return PathResolution.failed(PathResolution.Failure.MISSING_EQUIPMENT);
        }
        current = entity.equipment().get(equipment.slot());
      } else {
        var subjectLocation = context.graph().location(current);
        if (subjectLocation.isPresent()) {
          return PathResolution.location(subjectLocation.get());
        }
        return context.location()
            .map(PathResolution::location)
            .orElseGet(() -> PathResolution.failed(PathResolution.Failure.MISSING_LOCATION));
      }
    }
    return PathResolution.subject(current);
  }

  private TargetPath relation(SubjectRelation relation) {
    return append(new RelationSegment(relation, 0));
  }

  private TargetPath append(Segment segment) {
    if (!segments.isEmpty() && segments.get(segments.size() - 1) == LocationSegment.INSTANCE) {
      throw new IllegalStateException("location is a terminal path segment");
    }
    List<Segment> copy = new ArrayList<>(segments);
    copy.add(segment);
    return new TargetPath(copy);
  }

  private sealed interface Segment permits RelationSegment, EquipmentSegment, LocationSegment {}

  private record RelationSegment(SubjectRelation relation, int index) implements Segment {
    private RelationSegment {
      Objects.requireNonNull(relation, "relation");
      if (index < 0) {
        throw new IllegalArgumentException("relation index must be >= 0");
      }
    }
  }
  private record EquipmentSegment(EquipmentSlotKey slot) implements Segment {
    private EquipmentSegment {
      Objects.requireNonNull(slot, "slot");
    }
  }


  private enum LocationSegment implements Segment {
    INSTANCE
  }
}
