package dev.mintychochip.registry;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Token record wrapping the Adventure key that identifies a registry. */
record RegistryKeyImpl<T>(@NotNull Key key) implements RegistryKey<T> {}
