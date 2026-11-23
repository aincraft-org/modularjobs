package dev.mintychochip.databag.condition;

import dev.mintychochip.databag.DataBag;

import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable root and evaluation-scoped inputs for subject-graph navigation
 * ({@link TargetPath}). Predicate {@link Condition#test(ConditionContext)} still
 * evaluates the snapshot model; Paper currently builds {@link ConditionContext}.
 */
public final class EvaluationContext {

  private final SubjectSnapshot root;
  private final @Nullable LocationSnapshot location;
  private final @Nullable LootSnapshot loot;
  private final SubjectGraph graph;
  private final byte[] extensionBytes;

  private EvaluationContext(Builder builder) {
    root = Objects.requireNonNull(builder.root, "root");
    location = builder.location;
    loot = builder.loot;
    graph = builder.graph == null ? SubjectGraph.builder(root).build() : builder.graph;
    extensionBytes = (builder.extensions == null ? DataBag.create() : builder.extensions).toBytes();
  }

  /**
   * Starts a builder. {@link Builder#root(SubjectSnapshot)} is required.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns an evaluation context whose root is {@link SubjectSnapshot#absent()}.
   *
   * @return an empty evaluation context
   */
  public static EvaluationContext absent() {
    return builder().root(SubjectSnapshot.absent()).build();
  }

  /**
   * @return the evaluation root subject
   */
  public SubjectSnapshot root() {
    return root;
  }

  /**
   * @return the fallback location when a subject has no graph location
   */
  public Optional<LocationSnapshot> location() {
    return Optional.ofNullable(location);
  }

  /**
   * @return loot-only inputs, if captured
   */
  public Optional<LootSnapshot> loot() {
    return Optional.ofNullable(loot);
  }

  /**
   * @return the relation graph for this evaluation
   */
  public SubjectGraph graph() {
    return graph;
  }

  /**
   * @return a decoded copy of the extension bag
   */
  public DataBag extensions() {
    return DataBag.fromBytes(extensionBytes);
  }

  public static final class Builder {
    private @Nullable SubjectSnapshot root;
    private @Nullable LocationSnapshot location;
    private @Nullable LootSnapshot loot;
    private @Nullable SubjectGraph graph;
    private @Nullable DataBag extensions;

    private Builder() {}

    /**
     * @param value evaluation root, required
     * @return this builder
     */
    public Builder root(SubjectSnapshot value) {
      root = Objects.requireNonNull(value, "value");
      return this;
    }

    /**
     * @param value fallback location, or {@code null}
     * @return this builder
     */
    public Builder location(@Nullable LocationSnapshot value) {
      location = value;
      return this;
    }

    /**
     * @param value loot-only inputs, or {@code null}
     * @return this builder
     */
    public Builder loot(@Nullable LootSnapshot value) {
      loot = value;
      return this;
    }

    /**
     * @param value relation graph, or {@code null} to use an empty graph of the root
     * @return this builder
     */
    public Builder graph(@Nullable SubjectGraph value) {
      graph = value;
      return this;
    }


    /**
     * @param value extension bag, or {@code null} for empty
     * @return this builder
     */
    public Builder extensions(@Nullable DataBag value) {
      extensions = value;
      return this;
    }

    /**
     * @return the immutable evaluation context
     */
    public EvaluationContext build() {
      return new EvaluationContext(this);
    }
  }
}
