package dev.mintychochip.protection;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

/**
 * SPI for resolving the owning player of a protected block. Installed by {@link
 * BlockProtectionAdapterProvider} to bridge third-party protection plugins.
 */
@FunctionalInterface
@Internal
public interface BlockProtectionAdapter {

  /** Returns the owner. */
  @NotNull
  Optional<UUID> getOwner(@NotNull Block block);
}
