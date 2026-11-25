package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.domain.JobResolver;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.service.JoinGate.JoinResult;
import dev.mintychochip.service.JoinRefusals;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /jobs swap <from> <to>}: puts an active job on hold and makes another job active in its
 * slot. The held job keeps its level and experience. Listeners of {@code JobSwapRequestEvent} may
 * refuse the swap (price, cooldown) and tell the player why.
 */
public final class SwapCommand implements JobsCommand {

  private static final String DEFAULT_NAMESPACE = "modularjobs";

  private final JobService jobService;
  private final JobResolver jobResolver;
  private final JoinGate joinGate;

  /** Creates the swap command with the job service, resolver, and join gate. */
  public SwapCommand(
      @NotNull JobService jobService,
      @NotNull JobResolver jobResolver,
      @NotNull JoinGate joinGate) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
    this.joinGate = joinGate;
  }

  @Override
  public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("swap")
        .requires(source -> source.getSender().hasPermission("jobs.command.swap"))
        .then(
            Commands.argument("from", StringArgumentType.string())
                .suggests(
                    (context, builder) -> {
                      if (context.getSource().getSender() instanceof Player player) {
                        for (PlayerJobState state :
                            jobService.getPlayerJobStates(player.getUniqueId())) {
                          builder.suggest(state.jobKey().key().value());
                        }
                      }
                      return builder.buildFuture();
                    })
                .then(
                    Commands.argument("to", StringArgumentType.string())
                        .suggests(
                            (context, builder) -> {
                              jobResolver.getPlainNames().forEach(builder::suggest);
                              return builder.buildFuture();
                            })
                        .executes(this::execute)));
  }

  private int execute(@NotNull CommandContext<CommandSourceStack> context) {
    if (!(context.getSource().getSender() instanceof Player player)) {
      Messages.send(
          context.getSource().getSender(), "<error>This command can only be used by players.");
      return Command.SINGLE_SUCCESS;
    }
    Job from = resolve(player, context.getArgument("from", String.class));
    Job to = resolve(player, context.getArgument("to", String.class));
    if (from == null || to == null) {
      return 0;
    }
    String playerId = player.getUniqueId().toString();
    if (from.jobKey().equals(to.jobKey())) {
      Messages.send(player, "<error>Name two different jobs.");
      return 0;
    }
    if (jobService.getPlayerJobState(playerId, from.key().toString()) == null) {
      Messages.send(
          player,
          "<neutral>You are not in</neutral> <secondary>"
              + from.getPlainName()
              + "</secondary><neutral>.</neutral>");
      return 0;
    }
    if (jobService.getPlayerJobState(playerId, to.key().toString()) != null) {
      Messages.send(
          player,
          "<neutral>You are already in</neutral> <secondary>"
              + to.getPlainName()
              + "</secondary><neutral>.</neutral>");
      return 0;
    }
    List<PlayerJobState> current = jobService.getPlayerJobStates(player.getUniqueId());
    JoinResult result = joinGate.canSwap(player, from, to, current);
    if (result != JoinResult.ALLOWED) {
      Messages.send(player, JoinRefusals.message(result, to, joinGate, current));
      return 0;
    }
    // The gate passed, so a false result is a swap listener's refusal (it tells the player why)
    // or a store failure.
    if (jobService.swapJob(playerId, from.key().toString(), to.key().toString())) {
      Messages.send(
          player,
          "<secondary>"
              + from.getPlainName()
              + "</secondary> <primary>is on hold.</primary> <secondary>"
              + to.getPlainName()
              + "</secondary> <primary>is active.</primary>");
    } else {
      Messages.send(player, "<neutral>The swap did not happen.");
    }
    return Command.SINGLE_SUCCESS;
  }

  private @Nullable Job resolve(@NotNull Player player, @NotNull String input) {
    Job job = jobResolver.resolveInNamespace(input, DEFAULT_NAMESPACE);
    if (job == null) {
      Messages.send(player, "<error>Job not found: " + input);
      List<String> suggestions = jobResolver.suggestSimilar(input, 3);
      if (!suggestions.isEmpty()) {
        Messages.send(player, "<neutral>Did you mean: " + String.join(", ", suggestions));
      }
    }
    return job;
  }
}
