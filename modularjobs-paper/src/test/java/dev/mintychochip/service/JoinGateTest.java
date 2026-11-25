package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.config.ProgressionLimitsConfig;
import dev.mintychochip.config.ProgressionLimitsConfig.SlotGroup;
import dev.mintychochip.service.JoinGate.JoinResult;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class JoinGateTest {

  private static final Set<String> NO_DISABLED_WORLDS = Set.of();

  /** The server's layout: two primary slots, one secondary slot. */
  private static final List<SlotGroup> PRIMARY_SECONDARY =
      List.of(
          new SlotGroup("primary", 2, Set.of("miner", "logger", "harvester")),
          new SlotGroup("secondary", 1, Set.of("fisher", "cook")));

  private static JoinGate gate(
      List<SlotGroup> groups, boolean worldRestriction, Set<String> worlds) {
    return new JoinGate(new ProgressionLimitsConfig(groups, List.of(), worldRestriction), worlds);
  }

  @Test
  void noSlotGroupsMeansUnlimited() {
    JoinGate gate = gate(List.of(), true, NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.ALLOWED,
        gate.canJoin(
            permittedPlayer(true, "world"),
            job("miner"),
            List.of(state("farmer"), state("builder"), state("fisher"))));
  }

  @Test
  void slotGroupsCountOnlyTheirOwnMembers() {
    JoinGate gate = gate(PRIMARY_SECONDARY, true, NO_DISABLED_WORLDS);
    // One primary + the secondary held: the second primary slot is still free.
    assertEquals(
        JoinResult.ALLOWED,
        gate.canJoin(
            permittedPlayer(true, "world"),
            job("logger"),
            List.of(state("miner"), state("fisher"))));
  }

  @Test
  void fullGroupRefusesAnotherMember() {
    JoinGate gate = gate(PRIMARY_SECONDARY, true, NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.SLOTS_FULL,
        gate.canJoin(
            permittedPlayer(true, "world"),
            job("harvester"),
            List.of(state("miner"), state("logger"))));
    assertEquals(
        JoinResult.SLOTS_FULL,
        gate.canJoin(permittedPlayer(true, "world"), job("cook"), List.of(state("fisher"))));
  }

  @Test
  void jobOutsideEveryGroupIsRefused() {
    JoinGate gate = gate(PRIMARY_SECONDARY, true, NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.NO_SLOT, gate.canJoin(permittedPlayer(true, "world"), job("hunter"), List.of()));
  }

  @Test
  void swapFreesTheGivenUpSlot() {
    JoinGate gate = gate(PRIMARY_SECONDARY, true, NO_DISABLED_WORLDS);
    List<PlayerJobState> full = List.of(state("miner"), state("logger"), state("fisher"));
    assertEquals(
        JoinResult.ALLOWED,
        gate.canSwap(permittedPlayer(true, "world"), job("logger"), job("harvester"), full));
    assertEquals(
        JoinResult.ALLOWED,
        gate.canSwap(permittedPlayer(true, "world"), job("fisher"), job("cook"), full));
  }

  @Test
  void swapIntoAnotherFullGroupIsRefused() {
    JoinGate gate = gate(PRIMARY_SECONDARY, true, NO_DISABLED_WORLDS);
    List<PlayerJobState> full = List.of(state("miner"), state("logger"), state("fisher"));
    // Giving up the secondary does not free a primary slot.
    assertEquals(
        JoinResult.SLOTS_FULL,
        gate.canSwap(permittedPlayer(true, "world"), job("fisher"), job("harvester"), full));
  }

  @Test
  void deniesWhenPerJobPermissionMissing() {
    JoinGate gate = gate(PRIMARY_SECONDARY, true, NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.PERMISSION_DENIED,
        gate.canJoin(permittedPlayer(false, "world"), job("miner"), List.of()));
  }

  @Test
  void permissionCheckLowercasesJobName() {
    JoinGate gate = gate(List.of(), true, NO_DISABLED_WORLDS);
    assertEquals(
        JoinResult.ALLOWED, gate.canJoin(permittedPlayer(true, "world"), job("Miner"), List.of()));
  }

  @Test
  void worldRestrictionRejectsDisabledWorldCaseInsensitively() {
    JoinGate gate = gate(List.of(), true, Set.of("nether"));
    assertEquals(
        JoinResult.WORLD_DENIED,
        gate.canJoin(permittedPlayer(true, "NETHER"), job("miner"), List.of()));
  }

  @Test
  void worldRestrictionAllowsNonDisabledWorld() {
    JoinGate gate = gate(List.of(), true, Set.of("nether"));
    assertEquals(
        JoinResult.ALLOWED, gate.canJoin(permittedPlayer(true, "world"), job("miner"), List.of()));
  }

  @Test
  void worldRestrictionDisabledBypassesWorldList() {
    JoinGate gate = gate(List.of(), false, Set.of("nether"));
    assertEquals(
        JoinResult.ALLOWED, gate.canJoin(permittedPlayer(true, "NETHER"), job("miner"), List.of()));
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

  private static @NotNull JobKey key(@NotNull String name) {
    return new JobKey(Key.key("modularjobs", name.toLowerCase(Locale.ROOT)));
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
                return key(name);
              }
              if (method.getName().equals("key")) {
                return key(name).key();
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static @NotNull PlayerJobState state(@NotNull String jobName) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return (PlayerJobState)
        java.lang.reflect.Proxy.newProxyInstance(
            loader,
            new Class<?>[] {PlayerJobState.class},
            (proxy, method, args) -> {
              if (method.getName().equals("jobKey")) {
                return key(jobName);
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
