package dev.mintychochip.profession.content;

import dev.mintychochip.JobNodeKey;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Normalized craft task owned by one job node (output key already resolved). */
public record CraftTaskSnapshot(
    @NotNull JobNodeKey nodeKey, @NotNull Key contextKey, @NotNull Key outputKey) {}
