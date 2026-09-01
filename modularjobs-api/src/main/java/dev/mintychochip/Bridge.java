package dev.mintychochip;

import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.event.EventBus;
import dev.mintychochip.registry.RegistryContainer;
import dev.mintychochip.service.ActionService;
import dev.mintychochip.service.BuffService;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.NodeHarvestService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.service.StationService;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Bridge. */
public interface Bridge {

  /** Bridge. */
  static @NotNull Bridge bridge() {
    return BridgeRuntime.get();
  }

  /** Registry container. */
  @NotNull
  RegistryContainer registryContainer();

  /** Condition factory. */
  @NotNull
  ConditionFactory conditionFactory();

  /** Boost factory. */
  @NotNull
  BoostFactory boostFactory();

  /** Timed boost data service. */
  @NotNull
  TimedBoostDataService timedBoostDataService();

  /** Economy. */
  @NotNull
  Optional<EconomyProvider> economy();

  /** Custom action registration and reporting service. */
  @NotNull
  ActionService actionService();

  /** Job service. */
  @NotNull
  JobService jobService();

  /** Profession service. */
  @NotNull
  ProfessionService professionService();

  /** Recipe service. */
  @NotNull
  RecipeService recipeService();

  /** Buff service. */
  @NotNull
  BuffService buffService();

  /** Station service. */
  @NotNull
  StationService stationService();

  /** Node harvest service. */
  @NotNull
  NodeHarvestService nodeHarvestService();

  /** Event bus. */
  @NotNull
  EventBus eventBus();
}
