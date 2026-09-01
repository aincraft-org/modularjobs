package dev.mintychochip.editor;

import dev.mintychochip.Job;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.common.editor.EditorMetadata;
import dev.mintychochip.common.editor.EditorPayload;
import dev.mintychochip.common.editor.JobData;
import dev.mintychochip.common.editor.PayableData;
import dev.mintychochip.common.editor.TaskData;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.RelationalJobTaskRepositoryImpl;
import dev.mintychochip.domain.model.JobTaskRecord;
import dev.mintychochip.domain.model.PayableRecord;
import dev.mintychochip.registry.RegistryContainer;
import dev.mintychochip.registry.RegistryKeys;
import dev.mintychochip.registry.RegistryView;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Editor service. */
public final class EditorService {

  private final JobService jobService;
  private final RelationalJobTaskRepositoryImpl jobTaskRepository;
  private final RestSessionClient restSessionClient;
  private final EditorSessionStore sessionStore;
  private final EditorConfig config;

  /** Editor service. */
  public EditorService(
      @NotNull JobService jobService,
      @NotNull RelationalJobTaskRepositoryImpl jobTaskRepository,
      @NotNull RestSessionClient restSessionClient,
      @NotNull EditorSessionStore sessionStore,
      @NotNull EditorConfig config) {
    this.jobService = jobService;
    this.jobTaskRepository = jobTaskRepository;
    this.restSessionClient = restSessionClient;
    this.sessionStore = sessionStore;
    this.config = config;
  }

  private record ExportDraft(@NotNull UUID playerId, @NotNull EditorPayload payload) {}

  /** Created export. */
  private record CreatedExport(
      @NotNull UUID playerId, @NotNull RestSessionClient.CreatedSession session) {}

  /** Export tasks. */
  public @NotNull CompletableFuture<ExportResult> exportTasks(
      @Nullable String jobKey, @NotNull UUID playerId) {
    return CompletableFuture.supplyAsync(
            () -> {
              String sessionToken = UUID.randomUUID().toString();

              Map<String, JobData> jobDataMap = new LinkedHashMap<>();
              if (jobKey != null) {
                Job job = getJobOrThrow(jobKey);
                JobNode node = getNodeOrThrow(job, jobKey);
                jobDataMap.put(node.nodeKey().asString(), buildJobData(node));
              } else {
                for (Job job : jobService.getJobs()) {
                  job.nodes().values().stream()
                      .sorted(java.util.Comparator.comparing(node -> node.nodeKey().asString()))
                      .forEach(
                          node -> jobDataMap.put(node.nodeKey().asString(), buildJobData(node)));
                }
              }

              List<String> actionTypes = getRegisteredActionTypes();
              List<String> payableTypes = getRegisteredPayableTypes();
              EditorMetadata metadata =
                  EditorMetadata.create(
                      Instant.now().toString(), playerId.toString(), sessionToken, getServerName());
              EditorPayload payload =
                  EditorPayload.create(metadata, jobDataMap, actionTypes, payableTypes);
              return new ExportDraft(playerId, payload);
            })
        .thenCompose(
            draft ->
                restSessionClient
                    .create(draft.payload())
                    .thenApply(created -> new CreatedExport(draft.playerId(), created)))
        .thenApply(
            export -> {
              RestSessionClient.CreatedSession created = export.session();
              EditorSession session =
                  new EditorSession(
                      created.sessionCode(),
                      created.token(),
                      export.playerId(),
                      Instant.now(),
                      created.expiresAt());
              sessionStore.store(session);
              String webEditorUrl =
                  editorUrl(
                      config.webEditorUrl(),
                      config.sessionApiUrl(),
                      created.sessionCode(),
                      created.token());
              return new ExportResult(created.sessionCode(), webEditorUrl, created.token());
            })
        .exceptionally(
            failure -> {
              Throwable error =
                  failure instanceof CompletionException completion && completion.getCause() != null
                      ? completion.getCause()
                      : failure;
              throw new EditorException("Failed to export tasks: " + error.getMessage(), error);
            });
  }

  /** Import tasks. */
  public @NotNull CompletableFuture<ImportResult> importTasks(
      @NotNull String sessionCode, @NotNull UUID playerId) {
    return CompletableFuture.supplyAsync(
        () -> {
          List<String> errors = new ArrayList<>();
          int tasksImported = 0;
          int tasksDeleted = 0;

          EditorSession session = sessionStore.getOwned(sessionCode, playerId).orElse(null);
          if (session == null) {
            errors.add(
                "Editor session is missing, expired, or belongs to another player; "
                    + "run /jobs editor again.");
            return new ImportResult(0, 0, errors);
          }

          try {
            EditorPayload payload =
                restSessionClient.fetchPayload(session.sessionCode(), session.token()).join();

            if (!session.token().equals(payload.metadata().sessionToken())) {
              errors.add("REST payload session token did not match the authenticated session.");
              return new ImportResult(0, 0, errors);
            }

            for (Map.Entry<String, JobData> entry : payload.jobs().entrySet()) {
              String requestedNodeKey = entry.getKey();
              Job job = getJobOrThrow(requestedNodeKey);
              JobNode node = getNodeOrThrow(job, requestedNodeKey);
              String nodeKey = node.nodeKey().asString();
              JobData jobData = entry.getValue();
              List<JobTaskRecord> existingTasks = jobTaskRepository.getAllRecords(nodeKey);
              Set<String> incomingKeys = new HashSet<>();

              for (TaskData taskData : jobData.tasks()) {
                String key = taskKey(nodeKey, taskData.actionTypeKey(), taskData.contextKey());
                incomingKeys.add(key);

                List<PayableRecord> payableRecords = new ArrayList<>();
                for (PayableData pd : taskData.payables()) {
                  payableRecords.add(
                      new PayableRecord(pd.type(), new BigDecimal(pd.amount()), null, null));
                }
                JobTaskRecord record =
                    new JobTaskRecord(
                        nodeKey, taskData.actionTypeKey(), taskData.contextKey(), payableRecords);

                if (jobTaskRepository.save(record)) {
                  tasksImported++;
                }
              }

              for (JobTaskRecord existing : existingTasks) {
                String key =
                    taskKey(existing.nodeKey(), existing.actionTypeKey(), existing.contextKey());
                if (!incomingKeys.contains(key)
                    && jobTaskRepository.delete(
                        existing.nodeKey(), existing.actionTypeKey(), existing.contextKey())) {
                  tasksDeleted++;
                }
              }
            }

            sessionStore.remove(session.sessionCode());
            return new ImportResult(tasksImported, tasksDeleted, errors);
          } catch (CompletionException e) {
            if (e.getCause() instanceof RestSessionClient.RestSessionException rest) {
              errors.add(
                  rest.expired() ? "Session expired; run /jobs editor again." : rest.getMessage());
              return new ImportResult(tasksImported, tasksDeleted, errors);
            }
            errors.add("Failed to import tasks: " + e.getMessage());
            return new ImportResult(tasksImported, tasksDeleted, errors);
          } catch (IllegalArgumentException | IllegalStateException e) {
            errors.add("Failed to import tasks: " + e.getMessage());
            return new ImportResult(tasksImported, tasksDeleted, errors);
          }
        });
  }

  @Contract(pure = true)
  static @NotNull String editorUrl(
      @NotNull String base, @NotNull String apiBase, @NotNull String code, @NotNull String token) {
    String normalized = base.replaceFirst("/+$", "") + "/";
    String encodedApi = encode(apiBase);
    return normalized + "?api=" + encodedApi + "&code=" + encode(code) + "#token=" + encode(token);
  }

  @Contract(pure = true)
  private static @NotNull String encode(@NotNull String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  @Contract(pure = true)
  private @NotNull String taskKey(
      @NotNull String jobKey, @NotNull String actionTypeKey, @NotNull String contextKey) {
    return jobKey + "|" + actionTypeKey + "|" + contextKey;
  }

  /** Builds editor data from one node's direct task definitions. */
  private @NotNull JobData buildJobData(@NotNull JobNode node) {
    List<TaskData> tasks =
        jobTaskRepository.getAllRecords(node.nodeKey().asString()).stream()
            .map(this::buildTaskData)
            .toList();
    return JobData.create(node.getPlainName(), tasks);
  }

  /** Builds editor task data without materializing inherited definitions on a child node. */
  private @NotNull TaskData buildTaskData(@NotNull JobTaskRecord task) {
    List<PayableRecord> payableRecords = task.payables() == null ? List.of() : task.payables();
    List<PayableData> payables =
        payableRecords.stream()
            .map(
                payable ->
                    PayableData.create(payable.payableTypeKey(), payable.amount().toPlainString()))
            .toList();
    return TaskData.create(task.actionTypeKey(), task.contextKey(), payables);
  }

  /** Gets all registered action type keys. */
  private @NotNull List<String> getRegisteredActionTypes() {
    RegistryView<ActionType> registry =
        RegistryContainer.registryContainer().getRegistry(RegistryKeys.ACTION_TYPES);
    return registry.stream().map(type -> type.key().toString()).collect(Collectors.toList());
  }

  /** Gets all registered payable type keys. */
  private @NotNull List<String> getRegisteredPayableTypes() {
    RegistryView<PayableType> registry =
        RegistryContainer.registryContainer().getRegistry(RegistryKeys.PAYABLE_TYPES);
    return registry.stream().map(type -> type.key().toString()).collect(Collectors.toList());
  }

  /** Gets the server name from Bukkit configuration. */
  private @Nullable String getServerName() {
    try {
      return Bukkit.getServer().getName();
    } catch (IllegalStateException e) {
      return null;
    }
  }

  /** Gets a job tree by node or root key, or throws an exception. */
  private @NotNull Job getJobOrThrow(@NotNull String jobKey) {
    String fullKey = namespaced(jobKey);
    return jobService.getJob(fullKey);
  }

  private static @NotNull JobNode getNodeOrThrow(@NotNull Job job, @NotNull String nodeKey) {
    String fullKey = namespaced(nodeKey);
    JobNode node = job.node(new JobNodeKey(Key.key(fullKey)));
    if (node == null) {
      throw new IllegalArgumentException("Job node not found: " + fullKey);
    }
    return node;
  }

  private static @NotNull String namespaced(@NotNull String key) {
    return key.contains(":") ? key : "modularjobs:" + key;
  }

  /** Exception thrown when editor operations fail. */
  public static final class EditorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Editor exception. */
    public EditorException(@NotNull String message, @NotNull Throwable cause) {
      super(message, cause);
    }
  }
}
