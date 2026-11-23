package dev.mintychochip.profession;

import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler.PayableContext;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.registry.RegistryContainer;
import dev.mintychochip.registry.RegistryKeys;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.ProfessionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Facade: §8.1 catalog + {@link JobService} player state (storage keys). */
public final class ProfessionServiceImpl implements ProfessionService {

  private static final Key EXPERIENCE_PAYABLE = Key.key("modularjobs", "experience");

  private final JobService jobService;
  private final ProfessionIndex professions;

  /** Profession service impl. */
  public ProfessionServiceImpl(
      @NotNull JobService jobService, @NotNull ProfessionIndex professions) {
    this.jobService = jobService;
    this.professions = professions;
  }

  @Override
  public @NotNull List<ProfessionDefinition> tracks() {
    return professions.tracks();
  }

  @Override
  public @NotNull Optional<ProfessionDefinition> resolve(@NotNull String idOrAlias) {
    return professions.resolve(idOrAlias);
  }

  @Override
  public @NotNull OptionalInt level(@NotNull UUID playerId, @NotNull String professionIdOrAlias) {
    return playerState(playerId, professionIdOrAlias)
        .map(p -> OptionalInt.of(p.level()))
        .orElseGet(OptionalInt::empty);
  }

  @Override
  public @NotNull Optional<BigDecimal> experience(
      @NotNull UUID playerId, @NotNull String professionIdOrAlias) {
    return playerState(playerId, professionIdOrAlias).map(PlayerJobState::experience);
  }

  @Override
  public boolean ensureTrack(@NotNull UUID playerId, @NotNull String professionIdOrAlias) {
    Optional<ProfessionDefinition> def = professions.resolve(professionIdOrAlias);
    if (def.isEmpty()) {
      return false;
    }
    String storageKey = def.get().storageKey();
    try {
      PlayerJobState existing = jobService.getPlayerJobState(playerId.toString(), storageKey);
      if (existing != null) {
        return true;
      }
    } catch (IllegalArgumentException ignored) {
      // no player state
    }
    return jobService.joinJob(playerId.toString(), storageKey);
  }

  @Override
  public boolean grantExperience(
      @NotNull UUID playerId, @NotNull String professionIdOrAlias, @NotNull BigDecimal amount) {
    if (amount.signum() <= 0) {
      return false;
    }
    Optional<PlayerJobState> state = playerState(playerId, professionIdOrAlias);
    if (state.isEmpty()) {
      return false;
    }
    PayableType experience =
        RegistryContainer.registryContainer()
            .getRegistry(RegistryKeys.PAYABLE_TYPES)
            .getOrThrow(EXPERIENCE_PAYABLE);
    experience
        .handler()
        .pay(
            new PayableContext(
                playerId, new Payable(experience, PayableAmount.create(amount)), state.get()));
    return true;
  }

  private @NotNull Optional<PlayerJobState> playerState(
      @NotNull UUID playerId, @NotNull String professionIdOrAlias) {
    Optional<ProfessionDefinition> def = professions.resolve(professionIdOrAlias);

    if (def.isEmpty()) {
      return Optional.empty();
    }
    String storageKey = def.get().storageKey();
    PlayerJobState direct = jobService.getPlayerJobState(playerId.toString(), storageKey);
    if (direct != null) {
      return Optional.of(direct);
    }
    return jobService.getPlayerJobStates(playerId).stream()
        .filter(p -> p.jobKey().key().value().equalsIgnoreCase(storageKey))
        .findFirst();
  }
}
