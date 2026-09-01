package dev.mintychochip.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.service.JobService;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Provides paginated job leaderboard entries with a short-lived read cache. */
public final class JobTopPageProvider {

  private static final int ENTRIES_PER_QUERY = 100;

  private final JobService jobService;
  private final Cache<Key, List<PlayerJobState>> readCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();

  /**
   * Creates the page provider backed by the job service.
   *
   * @param jobService service used to load player states for a job key
   */
  public JobTopPageProvider(@NotNull JobService jobService) {
    this.jobService = jobService;
  }

  /**
   * Returns the requested page of a job's leaderboard, backed by a short-lived cache of the most
   * recent {@value ENTRIES_PER_QUERY} player states.
   *
   * @param jobKey job whose leaderboard is requested
   * @param pageNumber 1-based requested page, clamped to the available range
   * @param pageSize maximum entries per page
   * @return the page; an empty first page if the job has no player states
   */
  public @NotNull Page<PlayerJobState> getPage(@NotNull Key jobKey, int pageNumber, int pageSize) {
    List<PlayerJobState> states =
        readCache.get(
            jobKey, ignoredKey -> jobService.getPlayerJobStates(jobKey, ENTRIES_PER_QUERY));

    if (states == null || states.isEmpty()) {
      return new Page<>(List.of(), 1, pageSize);
    }

    int total = Math.min(ENTRIES_PER_QUERY, states.size());
    int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
    int clamped = Math.min(Math.max(pageNumber, 1), totalPages);

    int from = (clamped - 1) * pageSize;
    int to = Math.min(from + pageSize, total);

    List<PlayerJobState> slice = states.subList(from, to);
    return new Page<>(slice, clamped, pageSize);
  }

  /**
   * Returns all cached leaderboard entries for the given job.
   *
   * @param jobKey job whose player states are requested
   * @return the cached states, or an empty list if none are available
   */
  public @NotNull List<PlayerJobState> getAllEntries(@NotNull Key jobKey) {
    List<PlayerJobState> states =
        readCache.get(
            jobKey, ignoredKey -> jobService.getPlayerJobStates(jobKey, ENTRIES_PER_QUERY));
    return states != null ? states : List.of();
  }
}
