package dev.mintychochip.profession.config;

import dev.mintychochip.Job;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobTask;
import dev.mintychochip.profession.content.CraftRecipeContentValidation;
import dev.mintychochip.profession.content.CraftRecipeValidationReport;
import dev.mintychochip.profession.content.CraftTaskSnapshot;
import dev.mintychochip.profession.content.CraftTaskWithoutRecipeFinding;
import dev.mintychochip.profession.content.RegisteredRecipeSnapshot;
import dev.mintychochip.profession.content.RegisteredRecipeWithoutTaskFinding;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.RecipeService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Collects craft tasks and registered recipes, then logs cross-validation findings. */
public final class CraftRecipeContentValidator {

  private static final Key CRAFT_ACTION = Key.key("modularjobs", "craft");

  private CraftRecipeContentValidator() {}

  /** Validate and log. */
  public static void validateAndLog(
      @NotNull Plugin plugin,
      @NotNull JobService jobService,
      @NotNull RecipeService recipeService,
      @NotNull CraftRecipeContentValidationSettings.Settings settings) {
    if (!settings.enabled()) {
      return;
    }

    CraftRecipeValidationReport report =
        CraftRecipeContentValidation.validate(
            collectCraftTasks(jobService), snapshotRecipes(recipeService));

    logFindings(plugin, report, settings);
    plugin.getLogger().info(summaryLine(report));
  }

  static @NotNull List<CraftTaskSnapshot> collectCraftTasks(@NotNull JobService jobService) {
    Map<TaskDefinitionKey, JobTask> definitions = new LinkedHashMap<>();
    for (Job job : jobService.getJobs()) {
      List<JobNode> nodes =
          job.nodes().values().stream()
              .sorted(Comparator.comparing(node -> node.nodeKey().asString()))
              .toList();
      for (JobNode node : nodes) {
        for (List<JobTask> tasks : jobService.getAllTasks(job, node.nodeKey()).values()) {
          for (JobTask task : tasks) {
            definitions.putIfAbsent(new TaskDefinitionKey(task.nodeKey(), task.key()), task);
          }
        }
      }
    }

    return definitions.values().stream()
        .filter(task -> CRAFT_ACTION.equals(task.actionTypeKey()))
        .map(task -> new CraftTaskSnapshot(task.nodeKey(), task.contextKey(), task.contextKey()))
        .toList();
  }

  private record TaskDefinitionKey(
      @NotNull dev.mintychochip.JobNodeKey nodeKey, @NotNull JobTask.TaskKey taskKey) {}

  private static @NotNull List<RegisteredRecipeSnapshot> snapshotRecipes(
      @NotNull RecipeService recipeService) {
    return recipeService.registeredDefinitions().stream()
        .map(
            definition ->
                new RegisteredRecipeSnapshot(
                    definition.id(),
                    definition.craftOutputKey(),
                    definition.professionId(),
                    definition.requiredLevel()))
        .toList();
  }

  private static void logFindings(
      @NotNull Plugin plugin,
      @NotNull CraftRecipeValidationReport report,
      @NotNull CraftRecipeContentValidationSettings.Settings settings) {
    if (settings.warnTasksWithoutRecipe()) {
      logTaskFindings(plugin, report.tasksWithoutRecipe(), settings.maxDetailLines());
    }
    if (settings.logRecipesWithoutTask()) {
      logRecipeFindings(plugin, report.recipesWithoutTask(), settings.maxDetailLines());
    }
  }

  private static void logTaskFindings(
      @NotNull Plugin plugin,
      @NotNull List<CraftTaskWithoutRecipeFinding> findings,
      int maxDetailLines) {
    logCapped(plugin, findings, maxDetailLines, CraftTaskWithoutRecipeFinding::message);
  }

  private static void logRecipeFindings(
      @NotNull Plugin plugin,
      @NotNull List<RegisteredRecipeWithoutTaskFinding> findings,
      int maxDetailLines) {
    if (findings.isEmpty()) {
      return;
    }
    int limit = maxDetailLines == 0 ? 0 : Math.min(findings.size(), maxDetailLines);
    for (int i = 0; i < limit; i++) {
      plugin.getLogger().info(findings.get(i).message());
    }
    if (maxDetailLines > 0 && findings.size() > maxDetailLines) {
      plugin
          .getLogger()
          .info(
              "... and "
                  + (findings.size() - maxDetailLines)
                  + " more craft-recipe content finding(s)");
    }
  }

  private static <T> void logCapped(
      @NotNull Plugin plugin,
      @NotNull List<T> findings,
      int maxDetailLines,
      @NotNull java.util.function.Function<T, String> message) {
    if (findings.isEmpty()) {
      return;
    }
    int limit = maxDetailLines == 0 ? 0 : Math.min(findings.size(), maxDetailLines);
    for (int i = 0; i < limit; i++) {
      plugin.getLogger().warning(message.apply(findings.get(i)));
    }
    if (maxDetailLines > 0 && findings.size() > maxDetailLines) {
      plugin
          .getLogger()
          .warning(
              "... and "
                  + (findings.size() - maxDetailLines)
                  + " more craft-recipe content finding(s)");
    }
  }

  static @NotNull String summaryLine(@NotNull CraftRecipeValidationReport report) {
    return "Craft recipe content validation: "
        + report.tasksWithoutRecipe().size()
        + " craft task(s) without recipe metadata, "
        + report.recipesWithoutTask().size()
        + " recipe(s) without craft task(s).";
  }
}
