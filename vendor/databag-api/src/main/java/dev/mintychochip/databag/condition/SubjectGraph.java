package dev.mintychochip.databag.condition;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable identity-based relationship graph for evaluation subjects. */
public final class SubjectGraph {

  private final SubjectSnapshot root;
  private final IdentityHashMap<SubjectSnapshot, Map<SubjectRelation, List<SubjectSnapshot>>>
      relations;
  private final IdentityHashMap<SubjectSnapshot, LocationSnapshot> locations;

  private SubjectGraph(Builder builder) {
    builder.rejectCycles();
    root = builder.root;
    relations = copyRelations(builder.relations);
    locations = new IdentityHashMap<>(builder.locations);
  }

  /**
   * Starts a graph builder rooted at {@code root}.
   *
   * @param root graph root used for cycle detection
   * @return a new builder
   */
  public static Builder builder(SubjectSnapshot root) {
    return new Builder(root);
  }

  /**
   * Returns the first related subject for {@code relation}, if any.
   *
   * @param from subject to walk from
   * @param relation edge to follow
   * @return the related subject, or empty
   */
  public Optional<SubjectSnapshot> related(SubjectSnapshot from, SubjectRelation relation) {
    return related(from, relation, 0);
  }

  /**
   * Returns the related subject at {@code index} for {@code relation}.
   *
   * @param from subject to walk from
   * @param relation edge to follow
   * @param index passenger-style index, {@code 0} for singleton relations
   * @return the related subject, or empty
   */
  public Optional<SubjectSnapshot> related(
      SubjectSnapshot from, SubjectRelation relation, int index) {
    if (index < 0) {
      return Optional.empty();
    }
    Map<SubjectRelation, List<SubjectSnapshot>> edges = relations.get(from);
    if (edges == null) {
      return Optional.empty();
    }
    List<SubjectSnapshot> targets = edges.get(relation);
    return targets == null || index >= targets.size()
        ? Optional.empty()
        : Optional.of(targets.get(index));
  }

  /**
   * Returns every subject related to {@code from} by {@code relation}.
   *
   * @param from subject to walk from
   * @param relation edge to follow
   * @return an immutable list, empty when no edge exists
   */
  public List<SubjectSnapshot> relatedSubjects(
      SubjectSnapshot from, SubjectRelation relation) {
    Map<SubjectRelation, List<SubjectSnapshot>> edges = relations.get(from);
    return edges == null ? List.of() : edges.getOrDefault(relation, List.of());
  }

  /**
   * Returns the location captured for {@code subject}, if any.
   *
   * @param subject graph node
   * @return the location, or empty
   */
  public Optional<LocationSnapshot> location(SubjectSnapshot subject) {
    return Optional.ofNullable(locations.get(subject));
  }

  /**
   * Returns a copy of this graph with one additional relation. The original
   * root is preserved for cycle detection.
   *
   * @param from edge source
   * @param relation edge kind
   * @param to edge target
   * @return a new graph
   */
  public SubjectGraph withRelation(
      SubjectSnapshot from, SubjectRelation relation, SubjectSnapshot to) {
    Builder builder = new Builder(root);
    relations.forEach((subject, edges) -> edges.forEach(
        (edge, targets) -> targets.forEach(target -> builder.relate(subject, edge, target))));
    locations.forEach(builder::locate);
    return builder.relate(from, relation, to).build();
  }

  private static IdentityHashMap<
      SubjectSnapshot, Map<SubjectRelation, List<SubjectSnapshot>>> copyRelations(
          IdentityHashMap<SubjectSnapshot, Map<SubjectRelation, List<SubjectSnapshot>>> source) {
    IdentityHashMap<SubjectSnapshot, Map<SubjectRelation, List<SubjectSnapshot>>> copy =
        new IdentityHashMap<>();
    source.forEach((subject, edges) -> {
      var edgeCopy = new java.util.EnumMap<SubjectRelation, List<SubjectSnapshot>>(
          SubjectRelation.class);
      edges.forEach((relation, targets) -> edgeCopy.put(relation, List.copyOf(targets)));
      copy.put(subject, Map.copyOf(edgeCopy));
    });
    return copy;
  }

  public static final class Builder {
    private final SubjectSnapshot root;
    private final IdentityHashMap<SubjectSnapshot, Map<SubjectRelation, List<SubjectSnapshot>>>
        relations = new IdentityHashMap<>();
    private final IdentityHashMap<SubjectSnapshot, LocationSnapshot> locations =
        new IdentityHashMap<>();

    private Builder(SubjectSnapshot root) {
      this.root = Objects.requireNonNull(root, "root");
    }

    /**
     * Adds a relation edge. Non-passenger relations may be set only once.
     *
     * @param from edge source
     * @param relation edge kind
     * @param to edge target
     * @return this builder
     */
    public Builder relate(
        SubjectSnapshot from, SubjectRelation relation, SubjectSnapshot to) {
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(relation, "relation");
      Objects.requireNonNull(to, "to");
      Map<SubjectRelation, List<SubjectSnapshot>> edges =
          relations.computeIfAbsent(from, ignored -> new java.util.EnumMap<>(SubjectRelation.class));
      List<SubjectSnapshot> targets =
          edges.computeIfAbsent(relation, ignored -> new ArrayList<>());
      if (relation != SubjectRelation.PASSENGER && !targets.isEmpty()) {
        throw new IllegalStateException("relation already set: " + relation);
      }
      targets.add(to);
      return this;
    }

    /**
     * Records the location of {@code subject}.
     *
     * @param subject graph node
     * @param location captured location
     * @return this builder
     */
    public Builder locate(SubjectSnapshot subject, LocationSnapshot location) {
      locations.put(
          Objects.requireNonNull(subject, "subject"),
          Objects.requireNonNull(location, "location"));
      return this;
    }

    /**
     * Builds an immutable graph, rejecting cycles.
     *
     * @return the graph
     */
    public SubjectGraph build() {
      return new SubjectGraph(this);
    }

    private void rejectCycles() {
      IdentityHashMap<SubjectSnapshot, Visit> states = new IdentityHashMap<>();
      IdentityHashMap<SubjectSnapshot, Boolean> subjects = new IdentityHashMap<>();
      subjects.put(root, Boolean.TRUE);
      relations.forEach((subject, edges) -> {
        subjects.put(subject, Boolean.TRUE);
        edges.values().forEach(targets -> targets.forEach(target -> subjects.put(target, Boolean.TRUE)));
      });
      for (SubjectSnapshot subject : subjects.keySet()) {
        if (states.get(subject) == null) {
          visit(subject, states);
        }
      }
    }

    private void visit(SubjectSnapshot subject, IdentityHashMap<SubjectSnapshot, Visit> states) {
      states.put(subject, Visit.ACTIVE);
      Map<SubjectRelation, List<SubjectSnapshot>> edges = relations.get(subject);
      if (edges != null) {
        for (List<SubjectSnapshot> targets : edges.values()) {
          for (SubjectSnapshot target : targets) {
            Visit state = states.get(target);
            if (state == Visit.ACTIVE) {
              throw new IllegalArgumentException("subject graph contains a cycle");
            }
            if (state == null) {
              visit(target, states);
            }
          }
        }
      }
      states.put(subject, Visit.DONE);
    }
  }

  private enum Visit {
    ACTIVE,
    DONE
  }
}
