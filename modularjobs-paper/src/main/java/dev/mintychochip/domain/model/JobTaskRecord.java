package dev.mintychochip.domain.model;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable task definition owned by one job node.
 *
 * @param nodeKey the owning job-node key
 * @param actionTypeKey the action type this task tracks
 * @param contextKey the context scoping the action
 * @param payables the complete reward definition for the task
 */
public record JobTaskRecord(
    @NotNull String nodeKey,
    @NotNull String actionTypeKey,
    @Nullable String contextKey,
    @Nullable List<PayableRecord> payables) {

  /** Composite persistence identity for a task owned by one job node. */
  public record JobTaskRecordKey(
      @NotNull String nodeKey, @NotNull String actionTypeKey, @Nullable String contextKey) {}
}
