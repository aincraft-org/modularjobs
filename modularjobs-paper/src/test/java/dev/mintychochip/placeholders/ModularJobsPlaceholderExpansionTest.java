package dev.mintychochip.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ModularJobsPlaceholderExpansionTest {

  private static final UUID PLAYER = UUID.randomUUID();

  @Test
  void exposesLevelAndExperiencePerJob() {
    JobService service = serviceWith(state("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("7", expansion.onRequest(offlinePlayer(), "level_miner"));
    assertEquals("150", expansion.onRequest(offlinePlayer(), "experience_miner"));
  }

  @Test
  void exposesJoinedJobCountAndJobList() {
    JobService service = serviceWith(state("miner", 7, "150"), state("farmer", 3, "50"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("2", expansion.onRequest(offlinePlayer(), "joinedjobcount"));
    assertEquals("miner,farmer", expansion.onRequest(offlinePlayer(), "jobs"));
  }

  @Test
  void exposesTotalLevels() {
    JobService service = serviceWith(state("miner", 7, "150"), state("farmer", 3, "50"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("10", expansion.onRequest(offlinePlayer(), "totallevels"));
  }

  @Test
  void exposesJobNameDescriptionAndMaxLevel() {
    JobService service = serviceWith(state("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("miner", expansion.onRequest(offlinePlayer(), "name_miner"));
    assertEquals("Mines ores", expansion.onRequest(offlinePlayer(), "description_miner"));
    assertEquals("100", expansion.onRequest(offlinePlayer(), "maxlevel_miner"));
  }

  @Test
  void exposesMaxExperienceForCurrentLevel() {
    JobService service = serviceWith(state("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    // experienceForLevel(8) = "800" from the proxy stub
    assertEquals("800", expansion.onRequest(offlinePlayer(), "maxexperience_miner"));
  }

  @Test
  void exposesIsinAndCanjoin() {
    JobService service = serviceWith(state("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("true", expansion.onRequest(offlinePlayer(), "isin_miner"));
    assertEquals("false", expansion.onRequest(offlinePlayer(), "isin_farmer"));
    assertEquals("false", expansion.onRequest(offlinePlayer(), "canjoin_miner"));
    assertEquals("true", expansion.onRequest(offlinePlayer(), "canjoin_farmer"));
  }

  @Test
  void exposesArchivedJobCountAndMaxJobs() {
    JobService service = serviceWith(state("miner", 7, "150"));
    // archivedcount proxy returns 1 via getArchivedPlayerJobStates
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("1", expansion.onRequest(offlinePlayer(), "archivedjobs"));
    assertEquals("1", expansion.onRequest(offlinePlayer(), "maxjobs"));
  }

  @Test
  void returnsEmptyForUnknownPlaceholder() {
    JobService service = serviceWith(state("miner", 7, "150"));
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("", expansion.onRequest(offlinePlayer(), "bogus_param"));
  }

  @Test
  void missingProgressionReturnsEmpty() {
    JobService service = serviceWith();
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(service);
    assertEquals("", expansion.onRequest(offlinePlayer(), "level_miner"));
  }

  @Contract(pure = true)
  private static @NotNull JobService serviceWith(@NotNull PlayerJobState... states) {
    return (JobService)
        java.lang.reflect.Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {JobService.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "getJobs" -> {
                  return jobsOf(states);
                }
                case "getPlayerJobStates" -> {
                  return List.of(states);
                }
                case "getArchivedPlayerJobStates" -> {
                  return List.of(states.length > 0 ? states[0] : null);
                }
                case "getPlayerJobState" -> {
                  String jobKey = (String) args[1];
                  for (PlayerJobState p : states) {
                    if (p.job().key().toString().endsWith(jobKey)) {
                      return p;
                    }
                  }
                  return null;
                }
                default -> {
                  return defaultValue(method.getReturnType());
                }
              }
            });
  }

  @Contract(pure = true)
  private static @NotNull List<Job> jobsOf(@NotNull PlayerJobState... states) {
    java.util.ArrayList<Job> jobs = new java.util.ArrayList<>();
    for (PlayerJobState p : states) {
      jobs.add(p.job());
    }
    return jobs;
  }

  @Contract(pure = true)
  private static @NotNull PlayerJobState state(
      @NotNull String name, int level, @NotNull String exp) {
    Job job = job(name);
    return (PlayerJobState)
        java.lang.reflect.Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {PlayerJobState.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "job" -> {
                  return job;
                }
                case "level" -> {
                  return level;
                }
                case "experience" -> {
                  return new BigDecimal(exp);
                }
                case "experienceForLevel" -> {
                  return new BigDecimal("100").multiply(BigDecimal.valueOf((Integer) args[0]));
                }
                default -> {
                  return defaultValue(method.getReturnType());
                }
              }
            });
  }

  @Contract(pure = true)
  private static @NotNull Job job(@NotNull String name) {
    return (Job)
        java.lang.reflect.Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {Job.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "getPlainName" -> {
                  return name;
                }
                case "jobKey" -> {
                  return new dev.mintychochip.JobKey(Key.key("modularjobs", name));
                }
                case "key" -> {
                  return Key.key("modularjobs", name);
                }
                case "maxLevel" -> {
                  return 100;
                }
                case "displayName" -> {
                  return net.kyori.adventure.text.Component.text(name);
                }
                case "description" -> {
                  return net.kyori.adventure.text.Component.text("Mines ores");
                }
                default -> {
                  return defaultValue(method.getReturnType());
                }
              }
            });
  }

  @Contract(pure = true)
  private static @NotNull org.bukkit.OfflinePlayer offlinePlayer() {
    return (org.bukkit.OfflinePlayer)
        java.lang.reflect.Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {org.bukkit.OfflinePlayer.class},
            (proxy, method, args) ->
                method.getName().equals("getUniqueId")
                    ? PLAYER
                    : defaultValue(method.getReturnType()));
  }

  @Contract(pure = true)
  private static @Nullable Object defaultValue(@NotNull Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == double.class) {
      return 0D;
    }
    return null;
  }
}
