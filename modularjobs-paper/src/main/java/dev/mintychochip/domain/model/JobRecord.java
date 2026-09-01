package dev.mintychochip.domain.model;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Flat configuration definition of one node in a job tree.
 *
 * <p>Presentation and lineage belong to this node. The loader normalizes the owning root's
 * tree-wide leveling and payout rules into each record before runtime assembly.
 *
 * @param jobKey the unique node key
 * @param displayName the human-readable node name
 * @param description the node description
 * @param maxLevel the owning tree's maximum level
 * @param levellingCurve the owning tree's experience-threshold expression
 * @param payableCurves the owning tree's payout-curve expressions
 * @param parentKey the direct parent node key, or {@code null} for a root node
 */
public record JobRecord(
    @NotNull String jobKey,
    @NotNull String displayName,
    @Nullable String description,
    int maxLevel,
    @NotNull String levellingCurve,
    @NotNull Map<String, String> payableCurves,
    @Nullable String parentKey) {}
