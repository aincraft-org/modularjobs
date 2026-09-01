package dev.mintychochip.profession;

import dev.mintychochip.profession.config.YamlRecipeDefinitionLoader;
import dev.mintychochip.service.BuffService;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.NodeHarvestService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.service.StationService;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/** Manual composition for the profession service surfaces. */
public final class ProfessionWiring {

  public final ProfessionService professionService;
  public final RecipeService recipeService;
  public final BuffService buffService;
  public final StationService stationService;
  public final NodeHarvestService nodeHarvestService;

  private ProfessionWiring(
      @NotNull ProfessionService professionService,
      @NotNull RecipeService recipeService,
      @NotNull BuffService buffService,
      @NotNull StationService stationService,
      @NotNull NodeHarvestService nodeHarvestService) {
    this.professionService = professionService;
    this.recipeService = recipeService;
    this.buffService = buffService;
    this.stationService = stationService;
    this.nodeHarvestService = nodeHarvestService;
  }

  /** Create. */
  public static @NotNull ProfessionWiring create(
      @NotNull JavaPlugin plugin, @NotNull JobService jobService) {
    ProfessionIndex professionIndex = YamlProfessionDefinitionLoader.load(plugin, jobService);
    ProfessionService professionService = new ProfessionServiceImpl(jobService, professionIndex);
    MemoryRecipeService recipeService = new MemoryRecipeService();
    YamlRecipeDefinitionLoader.load(plugin, recipeService);
    return new ProfessionWiring(
        professionService,
        recipeService,
        new MemoryBuffService(),
        new StubStationService(),
        new StubNodeHarvestService());
  }
}
