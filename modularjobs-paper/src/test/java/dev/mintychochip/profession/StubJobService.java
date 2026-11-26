package dev.mintychochip.profession;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.service.JobService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class StubJobService implements JobService {

  private final List<Job> jobs;
  @Nullable PlayerJobState progression;
  @Nullable String lastProgressionKey;
  @Nullable String lastJoinedKey;
  boolean joinResult = true;

  private StubJobService(@NotNull List<Job> jobs) {
    this.jobs = List.copyOf(jobs);
  }

  static @NotNull StubJobService withJobs(@NotNull String... storageKeys) {
    return new StubJobService(
        Arrays.stream(storageKeys)
            .map(key -> (Job) new StubJob(new JobKey(Key.key("modularjobs", key))))
            .toList());
  }

  @Override
  public @NotNull List<Job> getJobs() {
    return jobs;
  }

  @Override
  public @NotNull Job getJob(@NotNull String jobKey) {
    String normalized = jobKey.contains(":") ? jobKey : "modularjobs:" + jobKey;
    return jobs.stream()
        .filter(job -> job.key().asString().equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown job: " + jobKey));
  }

  @Override
  public @Nullable JobTask getTask(
      @NotNull Job job,
      @NotNull JobNodeKey nodeKey,
      @NotNull ActionType type,
      @NotNull Context context) {
    return null;
  }

  @Override
  public @NotNull Map<ActionType, List<JobTask>> getAllTasks(
      @NotNull Job job, @NotNull JobNodeKey nodeKey) {
    return Map.of();
  }

  @Override
  public boolean update(@NotNull PlayerJobState state) {
    return false;
  }

  @Override
  public boolean joinJob(@NotNull String playerId, @NotNull String jobKey) {
    lastJoinedKey = jobKey;
    return joinResult;
  }

  @Override
  public boolean leaveJob(@NotNull String playerId, @NotNull String jobKey) {
    return false;
  }

  @Override
  public @Nullable PlayerJobState getPlayerJobState(
      @NotNull String playerId, @NotNull String jobKey) {
    lastProgressionKey = jobKey;
    return progression;
  }

  @Override
  public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID playerId) {
    return List.of();
  }

  @Override
  public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull Key jobKey, int limit) {
    return List.of();
  }

  @Override
  public @NotNull List<PlayerJobState> getArchivedPlayerJobStates(@NotNull UUID playerId) {
    return List.of();
  }

  private record StubJob(@NotNull JobKey jobKey) implements Job {
    @Override
    public @NotNull JobNode rootNode() {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Map<JobNodeKey, JobNode> nodes() {
      return Map.of();
    }

    @Override
    public @NotNull LevelingCurve levelingCurve() {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Map<Key, PayableCurve> payableCurves() {
      return Map.of();
    }

    @Override
    public int maxLevel() {
      return 1;
    }
  }
}
