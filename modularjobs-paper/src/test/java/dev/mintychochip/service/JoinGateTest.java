package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.config.ProgressionLimitsConfig;
import dev.mintychochip.service.JoinGate.JoinResult;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class JoinGateTest {

  private static final Set<String> NO_DISABLED_WORLDS = Set.of();

  @Test
  void allowsWhenUnderLimitAndPermitted() {
    JoinGate gate =
        new JoinGate(new ProgressionLimitsConfig(2, List.of(), true), NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.ALLOWED, gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of()));
  }

  @Test
  void deniesWhenAtMaxJobs() {
    JoinGate gate =
        new JoinGate(new ProgressionLimitsConfig(1, List.of(), true), NO_DISABLED_WORLDS);
    PlayerJobState existing = state(job("farmer"));
    assertEquals(
        JoinResult.MAX_JOBS,
        gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of(existing)));
  }

  @Test
  void deniesWhenPerJobPermissionMissing() {
    JoinGate gate =
        new JoinGate(new ProgressionLimitsConfig(5, List.of(), true), NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.PERMISSION_DENIED,
        gate.canJoin(permittedPlayer(false, "world"), job("miner"), List.of()));
  }

  @Test
  void unlimitedMaxJobsDoesNotGate() {
    JoinGate gate =
        new JoinGate(new ProgressionLimitsConfig(0, List.of(), true), NO_DISABLED_WORLDS);
    PlayerJobState a = state(job("farmer"));
    PlayerJobState b = state(job("builder"));
    assertEquals(
        JoinResult.ALLOWED,
        gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of(a, b)));
  }

  @Test
  void permissionCheckLowercasesJobName() {
    JoinGate gate =
        new JoinGate(new ProgressionLimitsConfig(5, List.of(), true), NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.ALLOWED, gate.canJoin(permittedPlayer(true, "world"), job("Miner"), List.of()));
  }

  @Test
  void worldRestrictionRejectsDisabledWorldCaseInsensitively() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(5, List.of(), true), Set.of("nether"));
    assertEquals(
        JoinResult.WORLD_DENIED,
        gate.canJoin(permittedPlayer(true, "NETHER"), job("miner"), List.of()));
  }

  @Test
  void worldRestrictionAllowsNonDisabledWorld() {
    JoinGate gate = new JoinGate(new ProgressionLimitsConfig(5, List.of(), true), Set.of("nether"));
    assertEquals(
        JoinResult.ALLOWED, gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of()));
  }

  @Test
  void worldRestrictionDisabledBypassesWorldList() {
    JoinGate gate =
        new JoinGate(new ProgressionLimitsConfig(5, List.of(), false), Set.of("nether"));
    assertEquals(
        JoinResult.ALLOWED, gate.canJoin(permittedPlayer(true, "NETHER"), job("miner"), List.of()));
  }

  @Test
  void defaultsLocaleRoundTrip() {
    // Guards against accidental locale-dependent permission building.
    assertEquals("jobs.join.miner", "jobs.join." + "Miner".toLowerCase(Locale.ROOT));
  }

  private static @NotNull Player permittedPlayer(boolean permitted, @NotNull String worldName) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return (Player)
        java.lang.reflect.Proxy.newProxyInstance(
            loader,
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
              if (method.getName().equals("hasPermission")) {
                return permitted;
              }
              if (method.getName().equals("getWorld")) {
                return world(worldName);
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static @NotNull Object world(@NotNull String name) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return java.lang.reflect.Proxy.newProxyInstance(
        loader,
        new Class<?>[] {org.bukkit.World.class},
        (proxy, method, args) -> {
          if (method.getName().equals("getName")) {
            return name;
          }
          return defaultValue(method.getReturnType());
        });
  }

  private static @NotNull Job job(@NotNull String name) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return (Job)
        java.lang.reflect.Proxy.newProxyInstance(
            loader,
            new Class<?>[] {Job.class},
            (proxy, method, args) -> {
              if (method.getName().equals("getPlainName")) {
                return name;
              }
              if (method.getName().equals("jobKey")) {
                return new dev.mintychochip.JobKey(
                    net.kyori.adventure.key.Key.key("modularjobs", name.toLowerCase(Locale.ROOT)));
              }
              if (method.getName().equals("key")) {
                return net.kyori.adventure.key.Key.key(
                    "modularjobs", name.toLowerCase(Locale.ROOT));
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static @NotNull PlayerJobState state(@NotNull Job job) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return (PlayerJobState)
        java.lang.reflect.Proxy.newProxyInstance(
            loader,
            new Class<?>[] {PlayerJobState.class},
            (proxy, method, args) -> {
              if (method.getName().equals("job")) {
                return job;
              }
              return defaultValue(method.getReturnType());
            });
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
