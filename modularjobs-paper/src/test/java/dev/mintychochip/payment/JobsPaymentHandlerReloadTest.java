package dev.mintychochip.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.boost.BoostDataCodec;
import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.container.Context.MaterialContext;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.TimedBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.TimedBoostDataService.ActiveBoostData;
import dev.mintychochip.databag.gson.GsonConditionSerializer;
import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.profession.RecipeExperienceDepreciationPolicy;
import dev.mintychochip.service.ItemBoostDataService;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Proves {@link JobsPaymentHandler} reloads state per payable so multi-XP awards accumulate. */
class JobsPaymentHandlerReloadTest {

  private Plugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void reloadStateUsesServiceOverSnapshot() {
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    PlayerJobState snapshot = state(player, BigDecimal.TEN);
    PlayerJobState reloaded = state(player, new BigDecimal("99"));
    AtomicInteger getCalls = new AtomicInteger();
    JobService service =
        new StubJobService() {
          @Override
          public @NotNull PlayerJobState getPlayerJobState(
              @NotNull String playerId, @NotNull String jobKey) {
            getCalls.incrementAndGet();
            return reloaded;
          }
        };
    JobsPaymentHandler handler = new JobsPaymentHandler(plugin, unusedBoostEngine(), service);
    PlayerJobState result =
        handler.reloadState(player.getUniqueId().toString(), "modularjobs:miner", snapshot);
    assertSame(reloaded, result);
    assertEquals(1, getCalls.get());
  }

  @Test
  void payReloadsStateForEachPayable() {
    AtomicInteger getStateCalls = new AtomicInteger();
    List<BigDecimal> xpSnapshotsAtPay = new ArrayList<>();
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());

    PlayerJobState p0 = state(player, new BigDecimal("0"));
    PlayerJobState p10 = state(player, new BigDecimal("10"));

    PayableType expType = experienceType(ctx -> xpSnapshotsAtPay.add(ctx.jobState().experience()));

    Payable pay1 = new Payable(expType, PayableAmount.create(new BigDecimal("10")));
    Payable pay2 = new Payable(expType, PayableAmount.create(new BigDecimal("10")));
    Job job = p0.job();
    ActionType blockBreak = actionType("block_break");
    JobTask task =
        new JobTask(
            job.rootNode().nodeKey(),
            blockBreak.key(),
            Key.key("minecraft", "stone"),
            List.of(pay1, pay2));

    JobService service =
        new StubJobService() {
          @Override
          public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID p) {
            return List.of(p0);
          }

          @Override
          public @NotNull JobTask getTask(
              @NotNull Job j,
              @NotNull JobNodeKey nodeKey,
              @NotNull ActionType type,
              @NotNull Context context) {
            return task;
          }

          @Override
          public @NotNull PlayerJobState getPlayerJobState(
              @NotNull String playerId, @NotNull String jobKey) {
            int n = getStateCalls.getAndIncrement();
            return n == 0 ? p0 : p10;
          }
        };

    // Offline player → BoostEngine.evaluate returns empty without needing services
    JobsPaymentHandler handler = new JobsPaymentHandler(plugin, unusedBoostEngine(), service);
    handler.pay(player, blockBreak, new MaterialContext(Key.key("minecraft", "stone")));

    assertEquals(2, getStateCalls.get(), "one reload per payable");
    assertEquals(2, xpSnapshotsAtPay.size());
    assertEquals(0, xpSnapshotsAtPay.get(0).compareTo(BigDecimal.ZERO));
    assertEquals(0, xpSnapshotsAtPay.get(1).compareTo(new BigDecimal("10")));
  }

  @Test
  void payAppliesTreeWideCurveForThePayableType() {
    List<BigDecimal> paid = new ArrayList<>();
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    PayableType experience = experienceType(ctx -> paid.add(ctx.payable().amount().value()));
    PlayerJobState state =
        state(
            player,
            BigDecimal.ZERO,
            Map.of(
                experience.key(), parameters -> parameters.base().multiply(BigDecimal.valueOf(2))));
    ActionType blockBreak = actionType("block_break");
    JobTask task =
        new JobTask(
            state.currentNode().nodeKey(),
            blockBreak.key(),
            Key.key("minecraft", "stone"),
            List.of(new Payable(experience, PayableAmount.create(BigDecimal.TEN))));
    JobService service =
        new StubJobService() {
          @Override
          public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID playerId) {
            return List.of(state);
          }

          @Override
          public @NotNull JobTask getTask(
              @NotNull Job job,
              @NotNull JobNodeKey nodeKey,
              @NotNull ActionType type,
              @NotNull Context context) {
            return task;
          }
        };

    new JobsPaymentHandler(plugin, unusedBoostEngine(), service)
        .pay(player, blockBreak, new MaterialContext(Key.key("minecraft", "stone")));

    assertEquals(List.of(new BigDecimal("20")), paid);
  }

  @Test
  void payAppliesRecipeDepreciationForCraftExperience() {
    List<BigDecimal> paid = new ArrayList<>();
    OfflinePlayer player = MockBukkitSupport.offlinePlayer(UUID.randomUUID());
    Key recipeId = Key.key("modularjobs", "masterwork_iron_sword");
    Key output = Key.key("minecraft", "iron_sword");

    RecordingRecipeService recipes = new RecordingRecipeService();
    recipes.registerDefinition(new RecipeDefinition(recipeId, "weaponsmithing", 25, 2, output));

    PayableType expType = experienceType(ctx -> paid.add(ctx.payable().amount().value()));
    Payable payable = new Payable(expType, PayableAmount.create(new BigDecimal("100")));
    PlayerJobState state = state(player, BigDecimal.ZERO);
    ActionType craft = actionType("craft");
    JobTask task =
        new JobTask(
            state.currentNode().nodeKey(),
            craft.key(),
            Key.key("minecraft", "iron_sword"),
            List.of(payable));

    JobService service =
        new StubJobService() {
          @Override
          public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID p) {
            return List.of(state);
          }

          @Override
          public @NotNull JobTask getTask(
              @NotNull Job job,
              @NotNull JobNodeKey nodeKey,
              @NotNull ActionType type,
              @NotNull Context context) {
            return task;
          }
        };

    RecipeExperienceDepreciationApplier depreciation =
        new RecipeExperienceDepreciationApplier(
            new RecipeExperienceDepreciationPolicy(true, 0, 10),
            recipes,
            new RecordingProfessionService(30));

    JobsPaymentHandler handler =
        new JobsPaymentHandler(plugin, unusedBoostEngine(), service, depreciation);
    handler.pay(player, craft, new Context.ItemContext(output, 1));

    assertEquals(output, recipes.lastCraftOutputLookup());
    assertEquals(1, paid.size());
    assertEquals(0, new BigDecimal("50").compareTo(paid.get(0)));
  }

  private static final class RecordingRecipeService implements RecipeService {
    private final Map<Key, RecipeDefinition> byCraftOutput = new HashMap<>();
    private Key lastCraftOutputLookup;

    @Override
    public boolean knows(@NotNull UUID playerId, @NotNull Key recipeId) {
      return false;
    }

    @Override
    public void grant(@NotNull UUID playerId, @NotNull Key recipeId) {}

    @Override
    public void revoke(@NotNull UUID playerId, @NotNull Key recipeId) {}

    @Override
    public @NotNull java.util.Set<Key> knownRecipes(@NotNull UUID playerId) {
      return java.util.Set.of();
    }

    @Override
    public boolean canCraft(@NotNull UUID playerId, @NotNull Key recipeId, int professionLevel) {
      return false;
    }

    @Override
    public void registerDefinition(@NotNull RecipeDefinition definition) {
      byCraftOutput.put(definition.craftOutputKey(), definition);
    }

    @Override
    public @NotNull java.util.Optional<RecipeDefinition> definition(@NotNull Key recipeId) {
      return java.util.Optional.empty();
    }

    @Override
    public @NotNull java.util.Optional<RecipeDefinition> definitionForCraftOutput(
        @NotNull Key outputMaterialKey) {
      lastCraftOutputLookup = outputMaterialKey;
      return java.util.Optional.ofNullable(byCraftOutput.get(outputMaterialKey));
    }

    @Nullable
    Key lastCraftOutputLookup() {
      return lastCraftOutputLookup;
    }
  }

  private static final class RecordingProfessionService implements ProfessionService {
    private final int level;

    RecordingProfessionService(int level) {
      this.level = level;
    }

    @Override
    public @NotNull List<dev.mintychochip.profession.ProfessionDefinition> tracks() {
      return List.of();
    }

    @Override
    public @NotNull java.util.Optional<dev.mintychochip.profession.ProfessionDefinition> resolve(
        @NotNull String idOrAlias) {
      return java.util.Optional.empty();
    }

    @Override
    public @NotNull OptionalInt level(@NotNull UUID playerId, @NotNull String professionIdOrAlias) {
      return OptionalInt.of(level);
    }

    @Override
    public @NotNull java.util.Optional<BigDecimal> experience(
        @NotNull UUID playerId, @NotNull String professionIdOrAlias) {
      return java.util.Optional.empty();
    }

    @Override
    public boolean ensureTrack(@NotNull UUID playerId, @NotNull String professionIdOrAlias) {
      return false;
    }
  }

  private static @NotNull PlayerJobState state(
      @NotNull OfflinePlayer player, @NotNull BigDecimal xp) {
    return state(player, xp, Map.of());
  }

  private static @NotNull PlayerJobState state(
      @NotNull OfflinePlayer player,
      @NotNull BigDecimal xp,
      @NotNull Map<Key, PayableCurve> payableCurves) {
    JobKey jobKey = new JobKey(Key.key("modularjobs", "miner"));
    JobNodeKey nodeKey = new JobNodeKey(jobKey.key());
    JobNode root =
        new JobNode() {
          @Override
          public @NotNull JobKey jobKey() {
            return jobKey;
          }

          @Override
          public @NotNull JobNodeKey nodeKey() {
            return nodeKey;
          }

          @Override
          public JobNodeKey parentKey() {
            return null;
          }

          @Override
          public @NotNull Component displayName() {
            return Component.text("Miner");
          }

          @Override
          public @NotNull String getPlainName() {
            return "miner";
          }

          @Override
          public @NotNull Component description() {
            return Component.empty();
          }
        };
    Job job =
        new Job() {
          @Override
          public @NotNull JobKey jobKey() {
            return jobKey;
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
          public @NotNull LevelingCurve levelingCurve() {
            return params -> BigDecimal.valueOf(params.level() * 100L);
          }

          @Override
          public @NotNull Map<Key, PayableCurve> payableCurves() {
            return payableCurves;
          }

          @Override
          public int maxLevel() {
            return 100;
          }
        };
    return new PlayerJobState() {
      @Override
      public @NotNull JobNode currentNode() {
        return root;
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
        return player.getUniqueId();
      }

      @Override
      public @NotNull BigDecimal experience() {
        return xp;
      }

      @Override
      public int level() {
        return 1;
      }

      @Override
      public @NotNull PlayerJobState withExperience(@NotNull BigDecimal experience) {
        return state(player, experience);
      }

      @Override
      public @NotNull PlayerJobState withCurrentNode(@NotNull JobNodeKey newNodeKey) {
        if (!nodeKey.equals(newNodeKey)) {
          throw new IllegalArgumentException();
        }
        return this;
      }
    };
  }

  private static @NotNull ActionType actionType(@NotNull String name) {
    Key key = Key.key("modularjobs", name);
    return new ActionType() {
      @Override
      public @NotNull Key key() {
        return key;
      }

      @Override
      public @NotNull String name() {
        return name;
      }
    };
  }

  private static @NotNull PayableType experienceType(@NotNull PayableHandler handler) {
    return new PayableType() {
      @Override
      public @NotNull PayableHandler handler() {
        return handler;
      }

      @Override
      public @NotNull Key key() {
        return Key.key("modularjobs", "experience");
      }
    };
  }

  private static @NotNull BoostEngine unusedBoostEngine() {
    ItemBoostDataService itemBoosts =
        new ItemBoostDataService(
            new BoostDataCodec(GsonConditionSerializer.gson(), BoostFactoryImpl.INSTANCE));
    TimedBoostDataService timedBoosts =
        new TimedBoostDataService() {
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
    return new BoostEngine(itemBoosts, timedBoosts, (playerId, jobKey) -> List.of());
  }

  private abstract static class StubJobService implements JobService {
    @Override
    public @NotNull List<Job> getJobs() {
      return List.of();
    }

    @Override
    public @NotNull Job getJob(@NotNull String jobKey) {
      throw new UnsupportedOperationException();
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
    public @Nullable PlayerJobState getPlayerJobState(
        @NotNull String playerId, @NotNull String jobKey) {
      return null;
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
