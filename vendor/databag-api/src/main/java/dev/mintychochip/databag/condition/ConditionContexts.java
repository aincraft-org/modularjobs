package dev.mintychochip.databag.condition;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Projects a subject-graph {@link EvaluationContext} onto the snapshot
 * {@link ConditionContext} consumed by {@link Condition#test(ConditionContext)}.
 */
public final class ConditionContexts {

  private ConditionContexts() {}

  /**
   * Builds a {@link ConditionContext} for {@code evaluation}'s root subject,
   * including related graph nodes.
   *
   * @param evaluation evaluation inputs
   * @return a snapshot equivalent of the root
   */
  public static ConditionContext from(EvaluationContext evaluation) {
    Objects.requireNonNull(evaluation, "evaluation");
    return convert(evaluation, evaluation.root(), new IdentityHashMap<>());
  }

  private static ConditionContext convert(
      EvaluationContext evaluation,
      SubjectSnapshot subject,
      IdentityHashMap<SubjectSnapshot, ConditionContext> seen) {
    ConditionContext cached = seen.get(subject);
    if (cached != null) {
      return cached;
    }
    ConditionContext.Builder builder =
        ConditionContext.builder().present(false).livingPresent(false);
    applySubject(builder, subject);
    applyLocation(
        builder,
        evaluation.graph().location(subject).orElse(evaluation.location().orElse(null)));
    builder.extras(evaluation.extensions());
    applyRelations(builder, evaluation, subject, seen);
    ConditionContext built = builder.build();
    seen.put(subject, built);
    return built;
  }

  private static void applySubject(ConditionContext.Builder builder, SubjectSnapshot subject) {
    switch (subject) {
      case PlayerSnapshot player -> {
        applyEntity(builder, player.entity());
        builder
            .present(true)
            .gameMode(player.gameMode())
            .hunger(player.hunger())
            .experience(player.experience())
            .xpLevel(player.level())
            .absorption(player.absorption())
            .airRemaining(player.airRemaining())
            .jobKeys(player.jobKeys())
            .scores(player.scores());
      }
      case EntitySnapshot entity -> applyEntity(builder, entity);
      case BlockSnapshot block ->
          builder.blockId(block.blockId()).blockProperties(block.properties());
      case ItemSubject item -> builder.itemSubject(item);
      case SubjectSnapshot.Absent ignored -> {}
    }
  }

  private static void applyEntity(ConditionContext.Builder builder, EntitySnapshot entity) {
    builder
        .livingPresent(true)
        .entityType(entity.entityType())
        .onFire(entity.onFire())
        .onGround(entity.onGround())
        .sneaking(entity.sneaking())
        .sprinting(entity.sprinting())
        .swimming(entity.swimming())
        .baby(entity.baby())
        .gliding(entity.gliding())
        .health(entity.health())
        .effects(entity.effects())
        .equipment(entity.equipment())
        .armorSet(entity.armorSet())
        .itemSubject(
            entity.equipment().getOrDefault(EquipmentSlotKey.MAINHAND, ItemSubject.empty()));
  }

  private static void applyLocation(
      ConditionContext.Builder builder, @Nullable LocationSnapshot location) {
    if (location == null) {
      return;
    }
    builder
        .worldName(location.worldName())
        .worldKey(location.worldKey())
        .biome(location.biome())
        .weather(location.weather())
        .fluid(location.fluid())
        .x(location.x())
        .y(location.y())
        .z(location.z())
        .lightLevel(location.lightLevel())
        .skyLight(location.skyLight())
        .blockLight(location.blockLight())
        .canSeeSky(location.canSeeSky())
        .dayTime(location.dayTime());
  }

  private static void applyRelations(
      ConditionContext.Builder builder,
      EvaluationContext evaluation,
      SubjectSnapshot subject,
      IdentityHashMap<SubjectSnapshot, ConditionContext> seen) {
    SubjectGraph graph = evaluation.graph();
    graph.related(subject, SubjectRelation.ATTACKER)
        .ifPresent(related -> builder.attacker(convert(evaluation, related, seen)));
    graph.related(subject, SubjectRelation.DIRECT_ATTACKER)
        .ifPresent(related -> builder.directAttacker(convert(evaluation, related, seen)));
    graph.related(subject, SubjectRelation.ATTACKING_PLAYER)
        .ifPresent(related -> builder.attackingPlayer(convert(evaluation, related, seen)));
    graph.related(subject, SubjectRelation.TARGET_ENTITY)
        .ifPresent(related -> builder.targetEntity(convert(evaluation, related, seen)));
    graph.related(subject, SubjectRelation.INTERACTING_ENTITY)
        .ifPresent(related -> builder.interactingEntity(convert(evaluation, related, seen)));
    graph.related(subject, SubjectRelation.VEHICLE)
        .ifPresent(related -> builder.vehicle(convert(evaluation, related, seen)));
    graph.related(subject, SubjectRelation.STANDING_ON)
        .ifPresent(related -> builder.standingOn(convert(evaluation, related, seen)));
    graph.related(subject, SubjectRelation.ITEM).ifPresent(related -> {
      if (related instanceof ItemSubject item) {
        builder.itemSubject(item);
      }
    });
    List<SubjectSnapshot> passengers = graph.relatedSubjects(subject, SubjectRelation.PASSENGER);
    if (!passengers.isEmpty()) {
      List<ConditionContext> converted = new ArrayList<>(passengers.size());
      for (SubjectSnapshot passenger : passengers) {
        converted.add(convert(evaluation, passenger, seen));
      }
      builder.passengers(converted);
    }
  }
}
