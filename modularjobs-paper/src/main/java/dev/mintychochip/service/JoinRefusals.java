package dev.mintychochip.service;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.config.ProgressionLimitsConfig.SlotGroup;
import dev.mintychochip.service.JoinGate.JoinResult;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Player-facing text for a refused {@link JoinGate} result, shared by commands and the GUI. */
public final class JoinRefusals {

  private JoinRefusals() {}

  /**
   * Returns the MiniMessage line for a refused join of {@code job}, or an empty string for {@link
   * JoinResult#ALLOWED}.
   *
   * @param current the player's active job states, used to report a full slot group
   */
  public static @NotNull String message(
      @NotNull JoinResult result,
      @NotNull Job job,
      @NotNull JoinGate gate,
      @NotNull List<PlayerJobState> current) {
    String name = "<secondary>" + job.getPlainName() + "</secondary>";
    return switch (result) {
      case SLOTS_FULL -> slotsFull(job, gate, current);
      case NO_SLOT -> name + " <error>has no job slot on this server.</error>";
      case PERMISSION_DENIED ->
          "<error>You do not have permission to join</error> " + name + "<error>.</error>";
      case WORLD_DENIED -> "<error>You cannot join jobs while in this world.";
      case ALLOWED -> "";
    };
  }

  private static @NotNull String slotsFull(
      @NotNull Job job, @NotNull JoinGate gate, @NotNull List<PlayerJobState> current) {
    SlotGroup group = gate.slotGroup(job);
    String detail =
        group == null
            ? "Your job slots are full."
            : "Your "
                + group.name()
                + " slots are full ("
                + gate.used(group, current)
                + "/"
                + group.slots()
                + ").";
    return "<error>"
        + detail
        + "</error> <neutral>Leave one of them</neutral> <accent>/jobs leave \\<job></accent>"
        + " <neutral>to put it on hold, then join.</neutral>";
  }
}
