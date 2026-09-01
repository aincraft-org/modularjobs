package dev.mintychochip.commands;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * One page of a paginated list (e.g. job leaderboard entries).
 *
 * @param data entries on this page
 * @param pageNumber 1-based page number (clamped to valid range)
 * @param size maximum entries per page
 */
public record Page<T>(@NotNull List<T> data, int pageNumber, int size) {}
