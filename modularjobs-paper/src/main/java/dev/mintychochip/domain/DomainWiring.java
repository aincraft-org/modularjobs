package dev.mintychochip.domain;

import dev.mintychochip.config.YamlConfiguration;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.MemoryJobRepositoryImpl.YamlRecordLoader;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.PluginResources;
import dev.mintychochip.service.JobPerkCatalog;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.util.KeyResolver;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Manual composition for domain-layer services (replaces Guice DomainModule). */
public final class DomainWiring {

  public static final String LIVE_REPOSITORY = "job_progression";
  public static final String ARCHIVE_REPOSITORY = "archive_job_progression";

  public final MemoryJobRepositoryImpl jobRepository;
  public final RelationalJobTaskRepositoryImpl jobTaskRepository;
  public final PlayerJobStateService playerJobStateService;
  public final JobService jobService;
  public final JobPerkCatalog perkCatalog;
  public final JobResolver jobResolver;

  private DomainWiring(
      @NotNull MemoryJobRepositoryImpl jobRepository,
      @NotNull RelationalJobTaskRepositoryImpl jobTaskRepository,
      @NotNull PlayerJobStateService playerJobStateService,
      @NotNull JobService jobService,
      @NotNull JobPerkCatalog perkCatalog,
      @NotNull JobResolver jobResolver) {
    this.jobRepository = jobRepository;
    this.jobTaskRepository = jobTaskRepository;
    this.playerJobStateService = playerJobStateService;
    this.jobService = jobService;
    this.perkCatalog = perkCatalog;
    this.jobResolver = jobResolver;
  }

  /**
   * Composes domain services, loading job trees and wiring player-state write-back stores.
   *
   * @param taskConnectionSource source for job-task/payable definitions
   * @param progressionConnectionSource source for live and archived player progression
   * @param resources registers player-state write-back flush hooks for disable
   */
  public static @NotNull DomainWiring create(
      @NotNull Plugin plugin,
      @NotNull ConnectionSource taskConnectionSource,
      @NotNull ConnectionSource progressionConnectionSource,
      @NotNull PluginResources resources,
      @NotNull Registry<ActionType> actionTypeRegistry,
      @NotNull Registry<PayableType> payableTypeRegistry,
      @NotNull KeyResolver keyResolver,
      @NotNull JoinGate joinGate) {
    YamlConfiguration jobsConfiguration = YamlConfiguration.create(plugin, "jobs.yml");
    YamlRecordLoader loader = new YamlRecordLoader();
    Map<String, JobRecord> records = loader.load(jobsConfiguration);
    MemoryJobRepositoryImpl jobRepository = new MemoryJobRepositoryImpl(records);
    final JobPerkCatalog perkCatalog = JobPerkCatalog.load(jobsConfiguration);

    RelationalJobTaskRepositoryImpl jobTaskRepository =
        new RelationalJobTaskRepositoryImpl(taskConnectionSource);

    WriteBackPlayerJobStateRepositoryImpl live =
        WriteBackPlayerJobStateRepositoryImpl.create(
            plugin,
            RelationalPlayerJobStateRepositoryImpl.create(
                jobRepository, progressionConnectionSource, LIVE_REPOSITORY),
            50,
            50,
            10,
            TimeUnit.SECONDS);
    WriteBackPlayerJobStateRepositoryImpl archive =
        WriteBackPlayerJobStateRepositoryImpl.create(
            plugin,
            RelationalPlayerJobStateRepositoryImpl.create(
                jobRepository, progressionConnectionSource, ARCHIVE_REPOSITORY),
            50,
            50,
            10,
            TimeUnit.SECONDS);
    resources.onFlush(live::flushPending);
    resources.onFlush(archive::flushPending);

    PlayerJobStateRepository liveView = live;
    PlayerJobStateRepository archiveView = archive;
    PlayerJobStateService playerJobStateService = new PlayerJobStateService(liveView, archiveView);
    JobService jobService =
        new JobServiceImpl(
            actionTypeRegistry,
            payableTypeRegistry,
            jobTaskRepository,
            keyResolver,
            jobRepository,
            playerJobStateService,
            joinGate,
            plugin);
    JobResolver jobResolver = new JobResolver(jobService);
    return new DomainWiring(
        jobRepository,
        jobTaskRepository,
        playerJobStateService,
        jobService,
        perkCatalog,
        jobResolver);
  }
}
