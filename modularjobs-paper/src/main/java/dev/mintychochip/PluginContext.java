package dev.mintychochip;

import com.google.gson.Gson;
import dev.mintychochip.action.ActionServiceImpl;
import dev.mintychochip.boost.BoostDataCodec;
import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.boost.ConsumableBoostController;
import dev.mintychochip.boost.ModularJobsBags;
import dev.mintychochip.boost.config.BoostSourceLoader;
import dev.mintychochip.commands.ApplyEditsCommand;
import dev.mintychochip.commands.ArchiveCommand;
import dev.mintychochip.commands.BoostCommand;
import dev.mintychochip.commands.BrowseCommand;
import dev.mintychochip.commands.EditorCommand;
import dev.mintychochip.commands.ExperienceCommand;
import dev.mintychochip.commands.InfoCommand;
import dev.mintychochip.commands.JobTopPageProvider;
import dev.mintychochip.commands.JobsCommand;
import dev.mintychochip.commands.JoinCommand;
import dev.mintychochip.commands.LeaveCommand;
import dev.mintychochip.commands.LevelCommand;
import dev.mintychochip.commands.ListCommand;
import dev.mintychochip.commands.StatsCommand;
import dev.mintychochip.commands.TopCommand;
import dev.mintychochip.commands.TreeEditorCommand;
import dev.mintychochip.commands.UpgradesCommand;
import dev.mintychochip.config.LevelUpCommandsConfig;
import dev.mintychochip.config.ProgressionLimitsConfig;
import dev.mintychochip.config.YamlConfiguration;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.databag.gson.GsonConditionSerializer;
import dev.mintychochip.domain.DomainWiring;
import dev.mintychochip.editor.EditorConfig;
import dev.mintychochip.editor.EditorService;
import dev.mintychochip.editor.EditorSessionStore;
import dev.mintychochip.editor.RestSessionClient;
import dev.mintychochip.editor.json.GsonProvider;
import dev.mintychochip.gui.JobBrowseGui;
import dev.mintychochip.gui.JobInfoGui;
import dev.mintychochip.gui.PaperSurfaces;
import dev.mintychochip.gui.PaperUiHost;
import dev.mintychochip.gui.StatsGui;
import dev.mintychochip.listener.AutoJoinListener;
import dev.mintychochip.listener.LevelUpCommandListener;
import dev.mintychochip.paper.event.LifecycleEventBus;
import dev.mintychochip.payable.PayableWiring;
import dev.mintychochip.payment.PaymentSettings;
import dev.mintychochip.payment.PaymentWiring;
import dev.mintychochip.placeholders.PlaceholderExpansionHandle;
import dev.mintychochip.profession.ProfessionWiring;
import dev.mintychochip.profession.config.CraftRecipeContentValidationSettings;
import dev.mintychochip.profession.config.CraftRecipeContentValidator;
import dev.mintychochip.protection.BlockOwnershipService;
import dev.mintychochip.protection.BlockProtectionAdapter;
import dev.mintychochip.protection.BlockProtectionAdapterProvider;
import dev.mintychochip.registry.ActionTypeRegistryProvider;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.registry.RegistryContainerImpl;
import dev.mintychochip.registry.RegistryKeys;
import dev.mintychochip.registry.SimpleRegistryImpl;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.DatabaseConfigSections;
import dev.mintychochip.repository.PluginResources;
import dev.mintychochip.repository.RelationalTimedBoostRepositoryImpl;
import dev.mintychochip.repository.SharedConnectionSources;
import dev.mintychochip.service.ActionService;
import dev.mintychochip.service.ItemBoostDataService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.service.LevelUpCommandExecutor;
import dev.mintychochip.service.PreferencesIntegration;
import dev.mintychochip.service.PreferencesService;
import dev.mintychochip.service.PreferencesServiceImpl;
import dev.mintychochip.service.TimedBoostDataServiceImpl;
import dev.mintychochip.upgrade.PlayerUpgradeRepository;
import dev.mintychochip.upgrade.SkillTree;
import dev.mintychochip.upgrade.UpgradeBoostDataService;
import dev.mintychochip.upgrade.UpgradeBoostDataServiceImpl;
import dev.mintychochip.upgrade.UpgradeEffectApplier;
import dev.mintychochip.upgrade.UpgradeLevelUpListener;
import dev.mintychochip.upgrade.UpgradePermissionManager;
import dev.mintychochip.upgrade.UpgradePermissionRestoreListener;
import dev.mintychochip.upgrade.UpgradeService;
import dev.mintychochip.upgrade.UpgradeServiceImpl;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.upgrade.config.UpgradeTreeLoader;
import dev.mintychochip.upgrade.rendering.UpgradeTreeGui;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorExporter;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorGui;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorNodeGui;
import dev.mintychochip.upgrade.rendering.editor.TreeEditorSettingsGui;
import dev.mintychochip.util.KeyResolver;
import dev.mintychochip.util.KeyResolvers;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.aincraft.event.EventBuses;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Manual composition root for ModularJobs (constructor wiring; no DI framework). */
public final class PluginContext {

  public final Bridge bridge;
  private final LifecycleEventBus eventBus;

  /** Primary payable ConnectionSource (also first entry in {@link #resources}). */
  public final ConnectionSource connectionSource;

  /** All DB sources + write-back flush hooks owned by this composition. */
  public final PluginResources resources;

  /** Shared native inventory host owned by this composition. */
  public final PaperUiHost paperUiHost;

  /** Shared native scoreboard and boss-bar surfaces owned by this composition. */
  public final PaperSurfaces paperSurfaces;

  public final UpgradeTreeLoader upgradeTreeLoader;
  public final Set<Listener> listeners;
  public final Set<JobsCommand> commands;
  @Nullable public final PlaceholderExpansionHandle placeholderExpansion;

  PluginContext(
      @NotNull Bridge bridge,
      @NotNull LifecycleEventBus eventBus,
      @NotNull ConnectionSource connectionSource,
      @NotNull PluginResources resources,
      @NotNull PaperUiHost paperUiHost,
      @NotNull PaperSurfaces paperSurfaces,
      @NotNull UpgradeTreeLoader upgradeTreeLoader,
      @NotNull Set<Listener> listeners,
      @NotNull Set<JobsCommand> commands,
      @Nullable PlaceholderExpansionHandle placeholderExpansion) {
    this.bridge = bridge;
    this.eventBus = eventBus;
    this.connectionSource = connectionSource;
    this.resources = resources;
    this.paperUiHost = paperUiHost;
    this.paperSurfaces = paperSurfaces;
    this.upgradeTreeLoader = upgradeTreeLoader;
    this.listeners = listeners;
    this.commands = commands;
    this.placeholderExpansion = placeholderExpansion;
  }

  /**
   * Flush write-backs and shut down every tracked ConnectionSource. Same path bootstrap uses on
   * disable.
   */
  public void shutdown() throws SQLException {
    paperUiHost.closeAll();
    paperSurfaces.closeAll();
    eventBus.close();
    resources.shutdown();
  }

  /** Create. */
  public static @NotNull PluginContext create(@NotNull JavaPlugin plugin) {
    PluginResources resources = new PluginResources();
    boolean created = false;
    try {
      PluginContext context = createInto(plugin, resources);
      created = true;
      return context;
    } finally {
      if (!created) {
        resources.closeQuietly();
      }
    }
  }

  /**
   * Composition body. Sources are tracked on {@code resources} as they open so callers (and {@link
   * #create}) can clean up on failure.
   */
  static @NotNull PluginContext createInto(
      @NotNull JavaPlugin plugin, @NotNull PluginResources resources) {
    final YamlConfiguration databaseConfig = YamlConfiguration.create(plugin, "database.yml");
    plugin.getSLF4JLogger().info("Loading database.yml, keys: {}", databaseConfig.getKeys(false));

    final ConfigurationSection payableSection =
        DatabaseConfigSections.requireSection(databaseConfig, "payable");
    final SharedConnectionSources sharedSources = new SharedConnectionSources(plugin, resources);
    final ConnectionSource connectionSource = sharedSources.getOrCreate(payableSection);

    ModularJobsBags.register();
    final KeyResolver keyResolver = KeyResolvers.create();
    final BoostFactory boostFactory = BoostFactoryImpl.INSTANCE;
    final ConditionFactory conditionFactory = BoostFactoryImpl.INSTANCE;
    final BoostDataCodec boostDataCodec =
        new BoostDataCodec(GsonConditionSerializer.gson(), boostFactory);
    final Gson gson = GsonProvider.create();

    final Registry<ActionType> actionTypeRegistry = ActionTypeRegistryProvider.create(plugin);

    // JobService holds a reference to the payable registry; types are registered after domain
    // construction so the experience handler can close over JobService.
    final Registry<PayableType> payableTypeRegistry = new SimpleRegistryImpl<>();

    final ProgressionLimitsConfig progressionLimits = ProgressionLimitsConfig.fromPlugin(plugin);
    final PaymentSettings paymentSettingsForJoin = PaymentSettings.fromPlugin(plugin);
    final JoinGate joinGate =
        new JoinGate(progressionLimits, paymentSettingsForJoin.disabledWorlds());

    final DomainWiring domain =
        DomainWiring.create(
            plugin,
            connectionSource,
            resources,
            actionTypeRegistry,
            payableTypeRegistry,
            keyResolver,
            joinGate);

    final PaperUiHost paperUiHost = new PaperUiHost();
    final PaperSurfaces paperSurfaces = new PaperSurfaces();

    // Soft-depend: register the XP bar color preference with the external Preferences plugin
    // when present; falls back to green when absent.
    final PreferencesIntegration.Wiring preferencesWiring = PreferencesIntegration.wire(plugin);
    if (preferencesWiring.onDisable() != null) {
      resources.onFlush(preferencesWiring.onDisable());
    }

    final PayableWiring payables =
        PayableWiring.create(
            plugin,
            domain.jobService,
            payableTypeRegistry,
            paperSurfaces,
            preferencesWiring.experienceBarColor());

    RegistryContainerImpl registryContainer = new RegistryContainerImpl();
    registryContainer.addRegistry(RegistryKeys.ACTION_TYPES.key(), actionTypeRegistry);
    registryContainer.addRegistry(RegistryKeys.PAYABLE_TYPES.key(), payableTypeRegistry);

    ConfigurationSection timedBoostSection =
        DatabaseConfigSections.requireSection(databaseConfig, "timed-boost");
    ConnectionSource timedBoostSource = sharedSources.getOrCreate(timedBoostSection);
    RelationalTimedBoostRepositoryImpl timedBoostRepository =
        new RelationalTimedBoostRepositoryImpl(plugin, timedBoostSource, boostDataCodec);
    resources.onFlush(timedBoostRepository::flushPending);
    final TimedBoostDataService timedBoostDataService =
        new TimedBoostDataServiceImpl(timedBoostRepository);
    final ItemBoostDataService itemBoostDataService = new ItemBoostDataService(boostDataCodec);

    final PreferencesService preferencesService = new PreferencesServiceImpl(plugin);

    ConfigurationSection upgradesSection =
        DatabaseConfigSections.sectionOrFallback(databaseConfig, "upgrades", payableSection);
    ConnectionSource upgradeConnection = sharedSources.getOrCreate(upgradesSection);
    PlayerUpgradeRepository playerUpgradeRepository =
        new PlayerUpgradeRepository(upgradeConnection);

    Registry<UpgradeTree> upgradeTreeRegistry = new SimpleRegistryImpl<>();
    Registry<SkillTree> skillTreeRegistry = new SimpleRegistryImpl<>();
    UpgradeTreeLoader upgradeTreeLoader =
        new UpgradeTreeLoader(
            plugin, gson, upgradeTreeRegistry, skillTreeRegistry, conditionFactory, boostFactory);
    upgradeTreeLoader.load();

    final ProfessionWiring professions = ProfessionWiring.create(plugin, domain.jobService);
    CraftRecipeContentValidator.validateAndLog(
        plugin,
        domain.jobService,
        professions.recipeService,
        CraftRecipeContentValidationSettings.fromPlugin(plugin));

    final UpgradePermissionManager permissionManager = new UpgradePermissionManager(plugin);
    UpgradeEffectApplier effectApplier =
        new UpgradeEffectApplier(permissionManager, professions.recipeService);
    final UpgradeBoostDataService upgradeBoostDataService =
        new UpgradeBoostDataServiceImpl(
            playerUpgradeRepository, upgradeTreeRegistry, skillTreeRegistry);
    UpgradeService upgradeService =
        new UpgradeServiceImpl(
            upgradeTreeRegistry,
            skillTreeRegistry,
            playerUpgradeRepository,
            domain.jobService,
            effectApplier);

    final UpgradeTreeGui upgradeTreeGui = new UpgradeTreeGui(paperUiHost, upgradeService);
    TreeEditorExporter treeEditorExporter = new TreeEditorExporter();
    TreeEditorNodeGui treeEditorNodeGui = new TreeEditorNodeGui(plugin, paperUiHost);
    TreeEditorSettingsGui treeEditorSettingsGui = new TreeEditorSettingsGui(plugin, paperUiHost);
    final TreeEditorGui treeEditorGui =
        new TreeEditorGui(
            plugin,
            paperUiHost,
            treeEditorExporter,
            upgradeTreeLoader,
            treeEditorNodeGui,
            treeEditorSettingsGui);

    Registry<BoostSource> boostSourceRegistry = new SimpleRegistryImpl<>();
    BoostSourceLoader boostSourceLoader =
        new BoostSourceLoader(plugin, gson, conditionFactory, boostFactory, boostSourceRegistry);
    boostSourceLoader.load();

    BlockProtectionAdapter protectionAdapter = BlockProtectionAdapterProvider.create();
    BlockOwnershipService blockOwnershipService = new BlockOwnershipService(protectionAdapter);

    final PaymentWiring payment =
        PaymentWiring.create(
            plugin,
            domain.jobService,
            itemBoostDataService,
            timedBoostDataService,
            upgradeBoostDataService,
            blockOwnershipService,
            professions.recipeService,
            professions.professionService);

    final ActionService actionService =
        new ActionServiceImpl(actionTypeRegistry, payment.paymentHandler);

    LevelUpCommandsConfig levelUpCommands = LevelUpCommandsConfig.fromPlugin(plugin);
    final LevelUpCommandExecutor levelUpCommandExecutor =
        new LevelUpCommandExecutor(
            levelUpCommands, command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));

    final EditorConfig editorConfig = EditorConfig.fromPlugin(plugin);

    JobBrowseGui jobBrowseGui =
        new JobBrowseGui(paperUiHost, domain.jobService, upgradeService, joinGate);
    final StatsGui statsGui = new StatsGui(paperUiHost);
    final JobTopPageProvider topPageProvider = new JobTopPageProvider(domain.jobService);

    JobInfoGui jobInfoGui = new JobInfoGui(paperUiHost, preferencesService, payables.renderer);
    InfoCommand infoCommand =
        new InfoCommand(
            domain.jobService,
            domain.jobResolver,
            preferencesService,
            payables.renderer,
            jobInfoGui);

    Set<JobsCommand> commands = new LinkedHashSet<>();
    commands.add(new JoinCommand(domain.jobService, domain.jobResolver, joinGate));
    commands.add(new ListCommand(domain.jobService));
    commands.add(new TopCommand(domain.jobService, topPageProvider, plugin, paperSurfaces));
    commands.add(new BrowseCommand(jobBrowseGui));
    commands.add(infoCommand);
    commands.add(new LeaveCommand(domain.jobService, domain.jobResolver));
    if (editorConfig.enabled()) {
      EditorSessionStore sessionStore = new EditorSessionStore(editorConfig);
      RestSessionClient restSessionClient = new RestSessionClient(editorConfig, gson);
      EditorService editorService =
          new EditorService(
              domain.jobService,
              domain.jobTaskRepository,
              restSessionClient,
              sessionStore,
              editorConfig);
      commands.add(new ApplyEditsCommand(editorService));
      commands.add(new EditorCommand(editorService, domain.jobService, domain.jobResolver));
    }
    commands.add(new StatsCommand(domain.jobService, statsGui));
    commands.add(new ArchiveCommand(domain.jobService));
    commands.add(
        new BoostCommand(
            boostSourceRegistry,
            timedBoostDataService,
            itemBoostDataService,
            boostSourceLoader,
            upgradeBoostDataService,
            domain.jobService));
    commands.add(new UpgradesCommand(upgradeService, domain.jobResolver, upgradeTreeGui));
    commands.add(
        new TreeEditorCommand(
            upgradeService, domain.jobResolver, treeEditorGui, upgradeTreeLoader));
    commands.add(new LevelCommand(domain.jobService, domain.playerJobStateService));
    commands.add(new ExperienceCommand(domain.jobService, domain.playerJobStateService));

    List<Listener> listenerList = new ArrayList<>();
    listenerList.add(paperUiHost);
    listenerList.addAll(payables.listeners);
    listenerList.addAll(payment.listeners);
    listenerList.add(new ConsumableBoostController(itemBoostDataService, timedBoostDataService));
    // Config-driven level-up commands run after payment listeners have played feedback.
    listenerList.add(new LevelUpCommandListener(levelUpCommandExecutor));
    // Auto-join configured jobs after perks are restored by upgrade listeners.
    listenerList.add(new AutoJoinListener(domain.jobService, progressionLimits));
    // Native PaperUiHost owns info/stats navigation and close callbacks.
    listenerList.add(new UpgradeLevelUpListener(upgradeService, skillTreeRegistry));
    // UpgradeTreeGui clicks are dispatched by the native PaperUiHost.
    listenerList.add(
        new UpgradePermissionRestoreListener(
            upgradeService, effectApplier, permissionManager, skillTreeRegistry));

    LifecycleEventBus eventBus = new LifecycleEventBus(EventBuses.create());
    Bridge bridge =
        new BridgeImpl(
            registryContainer,
            actionService,
            domain.jobService,
            professions.professionService,
            professions.recipeService,
            professions.buffService,
            professions.stationService,
            professions.nodeHarvestService,
            payables.economyProvider,
            conditionFactory,
            boostFactory,
            timedBoostDataService,
            eventBus);

    // Soft-depend: only loads ModularJobsPlaceholderExpansion (and PAPI types) when present
    PlaceholderExpansionHandle placeholderExpansion =
        PlaceholderExpansionHandle.tryCreate(domain.jobService);

    return new PluginContext(
        bridge,
        eventBus,
        connectionSource,
        resources,
        paperUiHost,
        paperSurfaces,
        upgradeTreeLoader,
        new LinkedHashSet<>(listenerList),
        commands,
        placeholderExpansion);
  }
}
