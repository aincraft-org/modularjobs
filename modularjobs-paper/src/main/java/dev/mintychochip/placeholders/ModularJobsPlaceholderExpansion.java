package dev.mintychochip.placeholders;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion exposing the player's ModularJobs state.
 *
 * <p>Loaded only when PlaceholderAPI is available; requests read the backing {@link JobService} and
 * return an empty value when no player state exists or the placeholder is unknown.
 *
 * <p>Supported placeholders (identifier {@code modular}):
 *
 * <ul>
 *   <li>{@code joinedjobcount}, {@code jobs}, {@code totallevels}, {@code maxjobs}, {@code
 *       archivedjobs}
 *   <li>{@code level_<job>}, {@code experience_<job>}, {@code maxexperience_<job>}, {@code
 *       maxlevel_<job>}, {@code name_<job>}, {@code description_<job>}, {@code isin_<job>}, {@code
 *       canjoin_<job>}
 * </ul>
 */
public final class ModularJobsPlaceholderExpansion extends PlaceholderExpansion {

  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final JobService jobService;
  private static final String VERSION = "1.2";

  /**
   * Creates an expansion backed by the supplied job service.
   *
   * @param jobService service used to resolve placeholder player-state values
   */
  public ModularJobsPlaceholderExpansion(@NotNull JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  @Contract(pure = true)
  public @NotNull String getIdentifier() {
    return "modular";
  }

  @Override
  @Contract(pure = true)
  public @NotNull String getAuthor() {
    return "ModularJobs contributors";
  }

  @Override
  @Contract(pure = true)
  public @NotNull String getVersion() {
    return VERSION;
  }

  @Override
  public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
    if (player == null) {
      return "";
    }
    UUID playerId = player.getUniqueId();
    String lower = params.toLowerCase(Locale.ROOT);

    // Player-level placeholders
    switch (lower) {
      case "joinedjobcount" -> {
        return Integer.toString(jobService.getPlayerJobStates(playerId).size());
      }
      case "jobs" -> {
        List<PlayerJobState> states = jobService.getPlayerJobStates(playerId);
        StringBuilder sb = new StringBuilder();
        for (PlayerJobState p : states) {
          if (sb.length() > 0) {
            sb.append(',');
          }
          sb.append(p.job().getPlainName());
        }
        return sb.toString();
      }
      case "totallevels" -> {
        int total = 0;
        for (PlayerJobState p : jobService.getPlayerJobStates(playerId)) {
          total += p.level();
        }
        return Integer.toString(total);
      }
      case "maxjobs" -> {
        return Integer.toString(jobService.getJobs().size());
      }
      case "archivedjobs" -> {
        return Integer.toString(jobService.getArchivedPlayerJobStates(playerId).size());
      }
      default -> {
        // fall through to job-level parsing
      }
    }

    // Job-level placeholders: <param>_<job>
    int underscore = lower.indexOf('_');
    if (underscore <= 0 || underscore == lower.length() - 1) {
      return "";
    }
    String param = lower.substring(0, underscore);
    String jobName = lower.substring(underscore + 1);
    PlayerJobState state = stateFor(playerId, jobName);
    if (state == null) {
      return switch (param) {
        case "isin" -> "false";
        case "canjoin" -> "true";
        default -> "";
      };
    }
    Job job = state.job();
    return switch (param) {
      case "level" -> Integer.toString(state.level());
      case "experience" -> state.experience().toPlainString();
      case "maxexperience" -> maxExperience(state);
      case "maxlevel" -> Integer.toString(job.maxLevel());
      case "name" -> job.getPlainName();
      case "description" -> PLAIN.serialize(job.description());
      case "isin" -> "true";
      case "canjoin" -> "false";
      default -> "";
    };
  }

  private @Nullable PlayerJobState stateFor(@NotNull UUID playerId, @NotNull String jobName) {
    for (Job job : jobService.getJobs()) {
      if (job.getPlainName().equalsIgnoreCase(jobName)) {
        return jobService.getPlayerJobState(playerId.toString(), job.key().toString());
      }
    }
    // Fallback: treat the token as a raw key suffix (e.g. modularjobs:miner)
    try {
      return jobService.getPlayerJobState(playerId.toString(), "modularjobs:" + jobName);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private @NotNull String maxExperience(@NotNull PlayerJobState state) {
    int level = state.level();
    BigDecimal forNext = state.experienceForLevel(level + 1);
    if (forNext == null) {
      return state.experience().toPlainString();
    }
    return forNext.toPlainString();
  }
}
