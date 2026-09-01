package dev.mintychochip.domain;

import dev.mintychochip.Bridge;
import dev.mintychochip.Job;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.JobTaskRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.event.JobJoinEvent;
import dev.mintychochip.event.JobLeaveEvent;
import dev.mintychochip.paper.event.PaperEventBridge;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.util.KeyResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default {@link JobService} implementation wiring complete job trees, inherited node tasks, and
 * player job states together.
 *
 * <p>Flat definitions are grouped by the in-memory {@link MemoryJobRepositoryImpl} loaded once from
 * {@code jobs.yml}. Tasks come from {@link RelationalJobTaskRepositoryImpl}; live and archived
 * state operations go through {@link PlayerJobStateService}.
 *
 * <p>The immutable runtime tree catalog is assembled once at construction. This service is safe to
 * share; connections and background tasks remain owned by its repositories.
 *
 * <p>Failure semantics: callers that look up jobs by key might throw unchecked {@link
 * IllegalArgumentException}/{@link IllegalStateException} rather than return a sentinel—see
 * individual methods, and {@link #getPlayerJobState(String, String)} returns {@code null} when the
 * player has no state for that job tree.
 */
final class JobServiceImpl implements JobService {

  private final Registry<ActionType> actionTypeRegistry;
  private final Registry<PayableType> payableTypeRegistry;
  private final RelationalJobTaskRepositoryImpl jobTaskRepository;
  private final KeyResolver keyResolver;
  private final MemoryJobRepositoryImpl jobRepository;
  private final PlayerJobStateService playerJobStateService;
  private final JoinGate joinGate;
  private final List<Job> jobs;
  private final Map<String, Job> jobsByRootKey;

  /**
   * Wires job trees, node tasks, and player states into a single service facade.
   *
   * @param actionTypeRegistry registry of known action types
   * @param payableTypeRegistry registry of known payable types
   * @param jobTaskRepository relational task store
   * @param keyResolver resolves {@link Context} to keys for task lookup
   * @param jobRepository in-memory job tree definitions
   * @param playerJobStateService live/archive player-state store facade
   * @param joinGate join-eligibility gate
   * @param plugin plugin for key namespaces and events
   */
  JobServiceImpl(
      Registry<ActionType> actionTypeRegistry,
      Registry<PayableType> payableTypeRegistry,
      RelationalJobTaskRepositoryImpl jobTaskRepository,
      KeyResolver keyResolver,
      MemoryJobRepositoryImpl jobRepository,
      PlayerJobStateService playerJobStateService,
      JoinGate joinGate,
      Plugin plugin) {
    this.actionTypeRegistry = actionTypeRegistry;
    this.payableTypeRegistry = payableTypeRegistry;
    this.jobTaskRepository = jobTaskRepository;
    this.keyResolver = keyResolver;
    this.jobRepository = jobRepository;
    this.playerJobStateService = playerJobStateService;
    this.joinGate = joinGate;
    List<Job> assembledJobs =
        jobRepository.getJobs().stream()
            .<Job>map(
                root -> JobImpl.fromRecords(root, jobRepository.loadTree(root.jobKey()), plugin))
            .toList();
    Map<String, Job> assembledByRoot = new LinkedHashMap<>();
    for (Job job : assembledJobs) {
      assembledByRoot.put(job.jobKey().asString(), job);
    }
    this.jobs = assembledJobs;
    this.jobsByRootKey = Collections.unmodifiableMap(assembledByRoot);
  }

  private @NotNull Job toJob(@NotNull JobRecord rootRecord) {
    Job job = jobsByRootKey.get(rootRecord.jobKey());
    if (job == null) {
      throw new IllegalStateException("Job tree was not assembled: " + rootRecord.jobKey());
    }
    return job;
  }

  private @Nullable PlayerJobState toState(@NotNull PlayerJobStateRecord record) {
    JobRecord rootRecord = jobRepository.rootFor(record.jobKey());
    if (rootRecord == null || !record.jobKey().equals(rootRecord.jobKey())) {
      return null;
    }
    try {
      return PersistenceConverters.fromRecord(record, toJob(rootRecord));
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  @Override
  public @NotNull List<Job> getJobs() {
    return jobs;
  }

  @Override
  public @NotNull Job getJob(@NotNull String jobKey) {
    JobRecord rootRecord = jobRepository.rootFor(jobKey);
    if (rootRecord == null) {
      throw new IllegalArgumentException("Unknown job node: " + jobKey);
    }
    return toJob(rootRecord);
  }

  @Override
  public @Nullable JobTask getTask(
      @NotNull Job job,
      @NotNull JobNodeKey nodeKey,
      @NotNull ActionType type,
      @NotNull Context context) {
    Key contextKey = keyResolver.resolve(context);
    if (contextKey == null) {
      throw new IllegalStateException(
          "No KeyResolver strategy registered for context type: "
              + context.getClass().getSimpleName());
    }
    List<JobNode> path = job.pathTo(nodeKey);
    for (int index = path.size() - 1; index >= 0; index--) {
      JobTaskRecord record =
          jobTaskRepository.load(
              path.get(index).nodeKey().asString(), type.key().toString(), contextKey.toString());
      if (record != null) {
        return toTask(record);
      }
    }
    return null;
  }

  @Override
  public @NotNull Map<ActionType, List<JobTask>> getAllTasks(
      @NotNull Job job, @NotNull JobNodeKey nodeKey) {
    List<JobTask> resolved =
        resolveInheritedTasks(
            job,
            nodeKey,
            inheritedNodeKey ->
                jobTaskRepository.getRecords(inheritedNodeKey.asString()).values().stream()
                    .flatMap(List::stream)
                    .map(this::toTask)
                    .toList());

    Map<ActionType, List<JobTask>> byAction = new LinkedHashMap<>();
    for (JobTask task : resolved) {
      ActionType type = actionTypeRegistry.getOrThrow(task.actionTypeKey());
      byAction.computeIfAbsent(type, ignored -> new ArrayList<>()).add(task);
    }
    byAction.replaceAll((ignored, tasks) -> List.copyOf(tasks));
    return Collections.unmodifiableMap(byAction);
  }

  private @NotNull JobTask toTask(@NotNull JobTaskRecord record) {
    return PersistenceConverters.fromRecord(
        record, keyString -> payableTypeRegistry.getOrThrow(Key.key(keyString)));
  }

  static @NotNull List<JobTask> resolveInheritedTasks(
      @NotNull Job job,
      @NotNull JobNodeKey nodeKey,
      @NotNull Function<JobNodeKey, List<JobTask>> nodeTaskLoader) {
    Map<JobTask.TaskKey, JobTask> resolved = new LinkedHashMap<>();
    for (JobNode node : job.pathTo(nodeKey)) {
      for (JobTask task : nodeTaskLoader.apply(node.nodeKey())) {
        if (!task.nodeKey().equals(node.nodeKey())) {
          throw new IllegalArgumentException(
              "Task owner does not match loaded job node: " + task.nodeKey().asString());
        }
        resolved.put(task.key(), task);
      }
    }
    return List.copyOf(resolved.values());
  }

  @Override
  public boolean update(@NotNull PlayerJobState state) {
    return playerJobStateService.save(PersistenceConverters.toRecord(state));
  }

  @Override
  public boolean joinJob(@NotNull String playerId, @NotNull String jobKey) {
    JobRecord rootRecord = jobRepository.rootFor(jobKey);
    if (rootRecord == null) {
      throw new IllegalArgumentException("failed to join job, the job does not exist");
    }

    UUID uuid = UUID.fromString(playerId);
    Player player = Bukkit.getPlayer(uuid);
    Job job = toJob(rootRecord);
    String treeKey = job.jobKey().asString();

    // Enforce join eligibility (max jobs, per-job permission, world restriction) when the
    // player is online. This is the single enforcement point shared by /jobs join and the GUI.
    if (player != null) {
      List<PlayerJobState> current =
          playerJobStateService.loadAllForPlayer(playerId, 100).stream()
              .map(this::toState)
              .filter(Objects::nonNull)
              .toList();
      JoinGate.JoinResult result = joinGate.canJoin(player, job, current);
      if (result != JoinGate.JoinResult.ALLOWED) {
        return false;
      }
    }

    PaperEventBridge events = new PaperEventBridge(Bridge.bridge().eventBus());

    // Try to restore from archive first (rejoin case)
    if (playerJobStateService.restore(playerId, treeKey)) {
      PlayerJobStateRecord restored = playerJobStateService.load(playerId, treeKey);
      PlayerJobState restoredState = restored == null ? null : toState(restored);
      if (restoredState == null) {
        return false;
      }
      events.publishJoin(new JobJoinEvent(restoredState, true), player);
      return true;
    }

    // Check if already in job
    PlayerJobStateRecord record = playerJobStateService.load(playerId, treeKey);
    if (record != null) {
      return false; // Already in job
    }

    // New players enter at the tree root with tree-wide starting experience.
    BigDecimal startExperience = job.levelingCurve().evaluate(new LevelingCurve.Parameters(1));
    PlayerJobState initialState = new PlayerJobStateImpl(uuid, job, startExperience);
    if (playerJobStateService.save(PersistenceConverters.toRecord(initialState))) {
      events.publishJoin(new JobJoinEvent(initialState, false), player);
      return true;
    }
    return false;
  }

  @Override
  public boolean leaveJob(@NotNull String playerId, @NotNull String jobKey) {
    JobRecord rootRecord = jobRepository.rootFor(jobKey);
    if (rootRecord == null) {
      return false;
    }
    PlayerJobStateRecord record = playerJobStateService.load(playerId, rootRecord.jobKey());
    if (record == null) {
      return false;
    }
    PlayerJobState state = toState(record);
    if (state == null) {
      return false;
    }

    Player player = Bukkit.getPlayer(state.playerId());
    new PaperEventBridge(Bridge.bridge().eventBus()).publishLeave(new JobLeaveEvent(state), player);

    return playerJobStateService.archive(playerId, rootRecord.jobKey());
  }

  @Override
  public @Nullable PlayerJobState getPlayerJobState(
      @NotNull String playerId, @NotNull String jobKey) {
    // Ensure jobKey has proper namespace
    String fullJobKey = jobKey.contains(":") ? jobKey : "modularjobs:" + jobKey;

    JobRecord rootRecord = jobRepository.rootFor(fullJobKey);
    if (rootRecord == null) {
      return null;
    }
    PlayerJobStateRecord record = playerJobStateService.load(playerId, rootRecord.jobKey());
    return record == null ? null : toState(record);
  }

  @Override
  public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID playerId) {
    return playerJobStateService.loadAllForPlayer(playerId.toString(), 100).stream()
        .map(this::toState)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull Key jobKey, int limit) {
    JobRecord rootRecord = jobRepository.rootFor(jobKey.toString());
    if (rootRecord == null) {
      return List.of();
    }
    return playerJobStateService.loadAllForJob(rootRecord.jobKey(), limit).stream()
        .map(this::toState)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  public @NotNull List<PlayerJobState> getArchivedPlayerJobStates(@NotNull UUID playerId) {
    return playerJobStateService.loadAllArchivedForPlayer(playerId.toString(), 100).stream()
        .map(this::toState)
        .filter(Objects::nonNull)
        .toList();
  }
}
