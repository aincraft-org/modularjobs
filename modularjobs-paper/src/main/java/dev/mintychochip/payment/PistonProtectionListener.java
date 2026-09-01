package dev.mintychochip.payment;

import dev.mintychochip.payment.ExploitService.ExploitProtectionType;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Moves protection timers with piston-pushed blocks so farms cannot clear timers by sliding blocks.
 */
final class PistonProtectionListener implements Listener {

  private final ExploitService exploitService;

  PistonProtectionListener(@NotNull ExploitService exploitService) {
    this.exploitService = exploitService;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onExtend(final @NotNull BlockPistonExtendEvent event) {
    if (!exploitService.settings().pistonMoveProtections()) {
      return;
    }
    moveBlocks(event.getBlocks(), event.getDirection());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onRetract(final @NotNull BlockPistonRetractEvent event) {
    if (!exploitService.settings().pistonMoveProtections()) {
      return;
    }
    if (!event.isSticky()) {
      return;
    }
    moveBlocks(event.getBlocks(), event.getDirection());
  }

  private void moveBlocks(@NotNull List<Block> blocks, @NotNull BlockFace dir) {
    // Reverse order prevents multi-block chains from overwriting each other.
    for (int i = blocks.size() - 1; i >= 0; i--) {
      Block from = blocks.get(i);
      Location toLoc = from.getLocation().clone().add(dir.getModX(), dir.getModY(), dir.getModZ());
      Block to = toLoc.getBlock();
      exploitService.transferProtection(ExploitProtectionType.PLACED, from, to);
      exploitService.transferProtection(ExploitProtectionType.WAX, from, to);
      exploitService.transferProtection(ExploitProtectionType.STRIP, from, to);
    }
  }
}
