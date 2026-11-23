package dev.mintychochip.databag.condition;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/** Immutable environmental state at one evaluation position. */
public record LocationSnapshot(
    @Nullable String worldName,
    @Nullable Key worldKey,
    @Nullable Key biome,
    @Nullable WeatherState weather,
    @Nullable Key fluid,
    @Nullable Double x,
    @Nullable Double y,
    @Nullable Double z,
    @Nullable Integer lightLevel,
    @Nullable Integer skyLight,
    @Nullable Integer blockLight,
    @Nullable Boolean canSeeSky,
    @Nullable Long dayTime) {}
