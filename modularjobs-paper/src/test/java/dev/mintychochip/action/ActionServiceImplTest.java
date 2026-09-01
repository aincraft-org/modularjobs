package dev.mintychochip.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.boost.BoostDataCodec;
import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.TimedBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.databag.gson.GsonConditionSerializer;
import dev.mintychochip.payment.BoostEngine;
import dev.mintychochip.payment.JobsPaymentHandler;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.registry.SimpleRegistryImpl;
import dev.mintychochip.service.ActionService;
import dev.mintychochip.service.ItemBoostDataService;
import dev.mintychochip.service.JobService;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ActionServiceImplTest {

  private Plugin plugin;
  private Registry<ActionType> registry;
  private ActionService service;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
    registry = new SimpleRegistryImpl<>();
    service = actionService(new StubJobService() {});
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void registerStoresAndReturnsCanonicalAction() {
    Key key = Key.key("myplugin", "quest_complete");

    ActionType registered = service.register(key, "Quest Complete");

    assertEquals(key, registered.key());
    assertEquals("Quest Complete", registered.name());
    assertSame(registered, registry.getOrThrow(key));
  }

  @Test
  void registerRejectsDuplicateAndPreservesOriginal() {
    Key key = Key.key("myplugin", "quest_complete");
    ActionType original = service.register(key, "Quest Complete");

    assertThrows(
        IllegalArgumentException.class, () -> service.register(key, "Conflicting Quest Action"));

    assertSame(original, registry.getOrThrow(key));
  }

  @Test
  void registerRejectsBlankNameWithoutMutatingRegistry() {
    Key key = Key.key("myplugin", "quest_complete");

    assertThrows(IllegalArgumentException.class, () -> service.register(key, "   "));

    assertFalse(registry.isRegistered(key));
  }

  @Test
  void registerRejectsNullKey() {
    assertThrows(NullPointerException.class, () -> service.register(null, "Quest Complete"));
    assertEquals(0, registry.stream().count());
  }

  @Test
  void registerRejectsNullName() {
    Key key = Key.key("myplugin", "quest_complete");

    assertThrows(NullPointerException.class, () -> service.register(key, null));
    assertFalse(registry.isRegistered(key));
  }

  @Test
  void reportRejectsUnknownAction() {
    ActionType unknown =
        new ActionTypeImpl("Quest Complete", Key.key("myplugin", "quest_complete"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.report(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                unknown,
                Key.key("myplugin", "first_quest")));
  }

  @Test
  void reportRejectsNullPlayerId() {
    ActionType action = service.register(Key.key("myplugin", "quest_complete"), "Quest Complete");

    assertThrows(
        NullPointerException.class,
        () ->
            service.report(
                null, action, new Context.KeyContext(Key.key("myplugin", "first_quest"))));
  }

  @Test
  void reportRejectsNullActionType() {
    assertThrows(
        NullPointerException.class,
        () ->
            service.report(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                (ActionType) null,
                new Context.KeyContext(Key.key("myplugin", "first_quest"))));
  }

  @Test
  void reportRejectsNullContext() {
    ActionType action = service.register(Key.key("myplugin", "quest_complete"), "Quest Complete");

    assertThrows(
        NullPointerException.class,
        () ->
            service.report(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), action, (Context) null));
  }

  @Test
  void reportUsesCanonicalActionAndKeyedContextThroughPaymentPipeline() {
    UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Key actionKey = Key.key("myplugin", "quest_complete");
    Key contextKey = Key.key("myplugin", "first_quest");
    AtomicReference<UUID> observedPlayerId = new AtomicReference<>();
    AtomicReference<ActionType> observedAction = new AtomicReference<>();
    AtomicReference<Context> observedContext = new AtomicReference<>();
    AtomicInteger payments = new AtomicInteger();
    Job job = testJob();
    PlayerJobState state = state(playerId, job, BigDecimal.ZERO);
    Payable payable = new Payable(payableType(payments), PayableAmount.create(BigDecimal.ONE));
    JobTask task = new JobTask(job.rootNode().nodeKey(), actionKey, contextKey, List.of(payable));
    JobService jobs =
        new StubJobService() {
          @Override
          public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID id) {
            observedPlayerId.set(id);
            return List.of(state);
          }

          @Override
          public @NotNull JobTask getTask(
              @NotNull Job ignored,
              @NotNull JobNodeKey ignoredNodeKey,
              @NotNull ActionType type,
              @NotNull Context context) {
            observedAction.set(type);
            observedContext.set(context);
            return task;
          }

          @Override
          public @NotNull PlayerJobState getPlayerJobState(
              @NotNull String ignoredPlayerId, @NotNull String ignoredJobKey) {
            return state;
          }
        };
    ActionService reportingService = actionService(jobs);
    ActionType canonical = reportingService.register(actionKey, "Quest Complete");
    ActionType nonCanonical = new ActionTypeImpl("Alias", actionKey);

    reportingService.report(playerId, nonCanonical, contextKey);

    assertEquals(playerId, observedPlayerId.get());
    assertSame(canonical, observedAction.get());
    assertEquals(new Context.KeyContext(contextKey), observedContext.get());
    assertEquals(1, payments.get());
  }

  private static @NotNull Job testJob() {
    JobKey key = new JobKey(Key.key("modularjobs", "tester"));
    JobNodeKey nodeKey = new JobNodeKey(key.key());
    JobNode root =
        new JobNode() {
          @Override
          public @NotNull JobKey jobKey() {
            return key;
          }

          @Override
          public @NotNull JobNodeKey nodeKey() {
            return nodeKey;
          }

          @Override
          public @Nullable JobNodeKey parentKey() {
            return null;
          }

          @Override
          public @NotNull Component displayName() {
            return Component.text("Tester");
          }

          @Override
          public @NotNull String getPlainName() {
            return "tester";
          }

          @Override
          public @NotNull Component description() {
            return Component.empty();
          }
        };
    return new Job() {
      @Override
      public @NotNull JobKey jobKey() {
        return key;
      }

      @Override
      public @NotNull JobNode rootNode() {
        return root;
      }

      @Override
      public @NotNull Map<JobNodeKey, JobNode> nodes() {
        return Map.of(nodeKey, root);
      }

      @Override
      public @NotNull dev.mintychochip.LevelingCurve levelingCurve() {
        return parameters -> BigDecimal.valueOf(parameters.level() * 100L);
      }

      @Override
      public @NotNull Map<Key, dev.mintychochip.PayableCurve> payableCurves() {
        return Map.of();
      }

      @Override
      public int maxLevel() {
        return 100;
      }
    };
  }

  private static @NotNull PlayerJobState state(
      @NotNull UUID playerId, @NotNull Job job, @NotNull BigDecimal experience) {
    return new PlayerJobState() {
      @Override
      public @NotNull JobNode currentNode() {
        return job.rootNode();
      }

      @Override
      public @NotNull BigDecimal experienceForLevel(int level) {
        return BigDecimal.valueOf(level * 100L);
      }

      @Override
      public @NotNull Job job() {
        return job;
      }

      @Override
      public @NotNull UUID playerId() {
        return playerId;
      }

      @Override
      public @NotNull BigDecimal experience() {
        return experience;
      }

      @Override
      public int level() {
        return 1;
      }

      @Override
      public @NotNull PlayerJobState withExperience(@NotNull BigDecimal newExperience) {
        return state(playerId, job, newExperience);
      }

      @Override
      public @NotNull PlayerJobState withCurrentNode(@NotNull JobNodeKey nodeKey) {
        if (!job.rootNode().nodeKey().equals(nodeKey)) {
          throw new IllegalArgumentException();
        }
        return this;
      }
    };
  }

  private static @NotNull PayableType payableType(@NotNull AtomicInteger payments) {
    return new PayableType() {
      @Override
      public @NotNull dev.mintychochip.container.PayableHandler handler() {
        return ignored -> payments.incrementAndGet();
      }

      @Override
      public @NotNull Key key() {
        return Key.key("myplugin", "reward");
      }
    };
  }

  private static @NotNull BoostEngine unusedBoostEngine() {
    ItemBoostDataService itemBoostDataService =
        new ItemBoostDataService(
            new BoostDataCodec(GsonConditionSerializer.gson(), BoostFactoryImpl.INSTANCE));
    return new BoostEngine(
        itemBoostDataService,
        unusedTimedBoostService(),
        (ignoredPlayerId, ignoredJobKey) -> List.of());
  }

  private static @NotNull TimedBoostDataService unusedTimedBoostService() {
    return new TimedBoostDataService() {
      @Override
      public @NotNull List<ActiveBoostData> findApplicableBoosts(@NotNull Target target) {
        return List.of();
      }

      @Override
      public @NotNull List<ActiveBoostData> findBoosts(@NotNull Target target) {
        return List.of();
      }

      @Override
      public <T extends TimedBoostData & SerializableBoostData> void addData(
          @NotNull T data, @NotNull Target target) {}

      @Override
      public boolean removeBoost(@NotNull Target target, @NotNull String sourceIdentifier) {
        return false;
      }
    };
  }

  private @NotNull ActionService actionService(@NotNull JobService jobs) {
    JobsPaymentHandler paymentHandler = new JobsPaymentHandler(plugin, unusedBoostEngine(), jobs);
    return new ActionServiceImpl(registry, paymentHandler);
  }

  private abstract static class StubJobService implements JobService {
    @Override
    public @NotNull List<Job> getJobs() {
      return List.of();
    }

    @Override
    public @NotNull Job getJob(@NotNull String jobKey) {
      throw new UnsupportedOperationException("not used");
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
      return true;
    }

    @Override
    public boolean joinJob(@NotNull String playerId, @NotNull String jobKey) {
      return false;
    }

    @Override
    public boolean leaveJob(@NotNull String playerId, @NotNull String jobKey) {
      return false;
    }

    @Override
    public @NotNull PlayerJobState getPlayerJobState(
        @NotNull String playerId, @NotNull String jobKey) {
      throw new UnsupportedOperationException("not used");
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
  }
}
