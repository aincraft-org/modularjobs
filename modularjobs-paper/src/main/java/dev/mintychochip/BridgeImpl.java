package dev.mintychochip;

import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.registry.RegistryContainer;
import dev.mintychochip.service.BuffService;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.NodeHarvestService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.service.StationService;
import dev.mintychochip.upgrade.UpgradeService;
import java.util.Optional;
import org.aincraft.api.event.EventBus;
import org.jetbrains.annotations.Nullable;

/** Immutable bridge assembled by the composition root for public plugin integrations. */
record BridgeImpl(
    RegistryContainer registryContainer,
    JobService jobService,
    ProfessionService professionService,
    UpgradeService upgradeService,
    RecipeService recipeService,
    BuffService buffService,
    StationService stationService,
    NodeHarvestService nodeHarvestService,
    @Nullable EconomyProvider economyProvider,
    ConditionFactory conditionFactory,
    BoostFactory boostFactory,
    TimedBoostDataService timedBoostDataService,
    EventBus eventBus)
    implements Bridge {

  /** Returns the configured economy provider, when an economy integration is available. */
  @Override
  public Optional<EconomyProvider> economy() {
    return Optional.ofNullable(economyProvider);
  }
}
