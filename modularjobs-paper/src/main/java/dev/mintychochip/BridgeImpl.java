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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Immutable bridge assembled by the composition root for public plugin integrations. */
record BridgeImpl(
    @NotNull RegistryContainer registryContainer,
    @NotNull ActionService actionService,
    @NotNull JobService jobService,
    @NotNull ProfessionService professionService,
    @NotNull RecipeService recipeService,
    @NotNull BuffService buffService,
    @NotNull StationService stationService,
    @NotNull NodeHarvestService nodeHarvestService,
    @Nullable EconomyProvider economyProvider,
    @NotNull ConditionFactory conditionFactory,
    @NotNull BoostFactory boostFactory,
    @NotNull TimedBoostDataService timedBoostDataService,
    @NotNull EventBus eventBus)
    implements Bridge {

  /** Returns the configured economy provider, when an economy integration is available. */
  @Override
  @Contract(pure = true)
  public @NotNull Optional<EconomyProvider> economy() {
    return Optional.ofNullable(economyProvider);
  }
}
