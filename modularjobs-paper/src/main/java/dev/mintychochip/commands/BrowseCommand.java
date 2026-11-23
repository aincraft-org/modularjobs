package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.gui.JobBrowseChatRenderer;
import dev.mintychochip.gui.JobBrowseDialogRenderer;
import dev.mintychochip.gui.JobBrowseGui;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.function.Consumer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Command to browse jobs with inventory, chat, or Paper Dialog renderers. */
public final class BrowseCommand implements JobsCommand {

  private final JobBrowseGui jobBrowseGui;
  private final JobBrowseChatRenderer chatRenderer;
  private final JobBrowseDialogRenderer dialogRenderer;

  /** Creates the browse command and its three explicit rendering modes. */
  public BrowseCommand(
      @NotNull JobBrowseGui jobBrowseGui,
      @NotNull JobBrowseChatRenderer chatRenderer,
      @NotNull JobBrowseDialogRenderer dialogRenderer) {
    this.jobBrowseGui = jobBrowseGui;
    this.chatRenderer = chatRenderer;
    this.dialogRenderer = dialogRenderer;
  }

  @Override
  public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("browse")
        .requires(source -> source.getSender().hasPermission("jobs.command.browse"))
        .then(
            Commands.literal("chat")
                .executes(context -> open(context.getSource(), chatRenderer::open)))
        .then(
            Commands.literal("dialog")
                .executes(context -> open(context.getSource(), dialogRenderer::open)))
        .then(
            Commands.literal("gui")
                .executes(context -> open(context.getSource(), jobBrowseGui::open)))
        .executes(context -> open(context.getSource(), jobBrowseGui::open));
  }

  private int open(@NotNull CommandSourceStack source, @NotNull Consumer<Player> renderer) {
    CommandSender sender = source.getSender();
    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }

    renderer.accept(player);
    return Command.SINGLE_SUCCESS;
  }
}
