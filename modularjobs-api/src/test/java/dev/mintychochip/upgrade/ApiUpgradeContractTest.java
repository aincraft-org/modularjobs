package dev.mintychochip.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ApiUpgradeContractTest {

  @Test
  void capabilityEffectValidatesSchemaAndCopiesPayload() {
    Map<String, String> payload = new java.util.HashMap<>();
    payload.put("mode", "fast");

    NodeEffect.CapabilityEffect effect =
        new NodeEffect.CapabilityEffect(Key.key("modularjobs:example"), 1, payload);

    payload.put("mode", "changed");
    assertEquals(Map.of("mode", "fast"), effect.payload());
    assertThrows(
        IllegalArgumentException.class,
        () -> new NodeEffect.CapabilityEffect(Key.key("modularjobs:example"), 0, Map.of()));
  }

  @Test
  void purchaseContractUsesCurrentServiceArguments() {
    UpgradeService service =
        new UpgradeService() {
          @Override
          public @NotNull java.util.Optional<UpgradeTree> getTree(@NotNull String jobKey) {
            return java.util.Optional.empty();
          }

          @Override
          public @NotNull java.util.Optional<SkillTree> getSkillTree(@NotNull String jobKey) {
            return java.util.Optional.empty();
          }

          @Override
          public @NotNull java.util.Collection<UpgradeTree> getAllTrees() {
            return java.util.List.of();
          }

          @Override
          public @Nullable PlayerUpgradeData getPlayerData(
              @NotNull String playerId, @NotNull String jobKey) {
            return null;
          }

          @Override
          public @NotNull java.util.Set<UpgradeNode> getAvailableNodes(
              @NotNull String playerId, @NotNull String jobKey) {
            return java.util.Set.of();
          }

          @Override
          public @NotNull UnlockResult unlock(
              @NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey) {
            return new UnlockResult.TreeNotFound(jobKey);
          }

          @Override
          public void awardSkillPoints(
              @NotNull String playerId, @NotNull String jobKey, int points) {}

          @Override
          public boolean resetUpgrades(@NotNull String playerId, @NotNull String jobKey) {
            return false;
          }

          @Override
          public @NotNull SkillTreeState getSkillTreeState(
              @NotNull String playerId, @NotNull String jobKey) {
            return SkillTreeState.empty(playerId, jobKey);
          }

          @Override
          public @NotNull PurchaseResult purchaseSkillLevel(
              @NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey) {
            return new PurchaseResult.TreeNotFound(jobKey);
          }

          @Override
          public @NotNull PurchaseResult purchaseMajor(
              @NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey) {
            return new PurchaseResult.TreeNotFound(jobKey);
          }

          @Override
          public boolean resetTree(@NotNull String playerId, @NotNull String jobKey) {
            return false;
          }

          @Override
          public void clearTreeState(@NotNull String playerId, @NotNull String jobKey) {}
        };

    assertFalse(service.resetTree("player", "job"));
  }
}
