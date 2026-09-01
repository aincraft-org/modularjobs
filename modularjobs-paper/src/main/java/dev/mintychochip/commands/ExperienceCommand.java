package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mintychochip.Job;
import dev.mintychochip.domain.PlayerJobStateService;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.service.JobService;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.math.BigDecimal;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /jobs experience} admin command: sets, adds, or subtracts experience for a player in a job
 * (requiring the {@code modularjobs.admin} permission).
 */
public final class ExperienceCommand implements JobsCommand {

  private final JobService jobService;
  private final PlayerJobStateService playerJobStateService;

  /**
   * Creates the experience command with the services that resolve jobs and persist player state.
   */
  public ExperienceCommand(
      @NotNull JobService jobService, @NotNull PlayerJobStateService playerJobStateService) {
    this.jobService = jobService;
    this.playerJobStateService = playerJobStateService;
  }

  /**
   * Builds the administrator-only {@code /jobs experience} command with the set, add, and subtract
   * subcommands.
   *
   * @return the Brigadier command tree for experience administration
   */
  @Override
  public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("experience")
        .requires(source -> source.getSender().hasPermission("modularjobs.admin"))
        .then(
            Commands.literal("set")
                .then(
                    Commands.argument("player", ArgumentTypes.player())
                        .then(
                            Commands.argument("job", StringArgumentType.string())
                                .suggests(
                                    (context, builder) -> {
                                      jobService.getJobs().stream()
                                          .map(job -> job.key().value())
                                          .forEach(builder::suggest);
                                      return builder.buildFuture();
                                    })
                                .then(
                                    Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(
                                            context ->
                                                executeSet(
                                                    context.getSource(),
                                                    context.getArgument(
                                                        "player",
                                                        PlayerSelectorArgumentResolver.class),
                                                    context.getArgument("job", String.class),
                                                    context.getArgument(
                                                        "amount", Double.class)))))))
        .then(
            Commands.literal("add")
                .then(
                    Commands.argument("player", ArgumentTypes.player())
                        .then(
                            Commands.argument("job", StringArgumentType.string())
                                .suggests(
                                    (context, builder) -> {
                                      jobService.getJobs().stream()
                                          .map(job -> job.key().value())
                                          .forEach(builder::suggest);
                                      return builder.buildFuture();
                                    })
                                .then(
                                    Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(
                                            context ->
                                                executeAdd(
                                                    context.getSource(),
                                                    context.getArgument(
                                                        "player",
                                                        PlayerSelectorArgumentResolver.class),
                                                    context.getArgument("job", String.class),
                                                    context.getArgument(
                                                        "amount", Double.class)))))))
        .then(
            Commands.literal("subtract")
                .then(
                    Commands.argument("player", ArgumentTypes.player())
                        .then(
                            Commands.argument("job", StringArgumentType.string())
                                .suggests(
                                    (context, builder) -> {
                                      jobService.getJobs().stream()
                                          .map(job -> job.key().value())
                                          .forEach(builder::suggest);
                                      return builder.buildFuture();
                                    })
                                .then(
                                    Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(
                                            context ->
                                                executeSubtract(
                                                    context.getSource(),
                                                    context.getArgument(
                                                        "player",
                                                        PlayerSelectorArgumentResolver.class),
                                                    context.getArgument("job", String.class),
                                                    context.getArgument(
                                                        "amount", Double.class)))))));
  }

  /** Sets the selected player's experience in the given job to the supplied amount. */
  private int executeSet(
      @NotNull CommandSourceStack source,
      @NotNull PlayerSelectorArgumentResolver playerResolver,
      @NotNull String jobKeyValue,
      double amount)
      throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Messages.send(sender, "<error>Player not found.</error>");
      return 0;
    }

    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Messages.send(
          sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    String playerId = targetPlayer.getUniqueId().toString();
    PlayerJobStateRecord currentRecord =
        playerJobStateService.load(playerId, job.jobKey().asString());

    if (currentRecord == null) {
      Messages.send(
          sender,
          "<secondary>"
              + targetPlayer.getName()
              + "</secondary> <error>is not in job</error> <accent>"
              + job.getPlainName()
              + "</accent>");
      return 0;
    }

    BigDecimal newExperience = BigDecimal.valueOf(amount);
    PlayerJobStateRecord newRecord =
        new PlayerJobStateRecord(
            playerId, currentRecord.jobKey(), currentRecord.currentNodeKey(), newExperience);

    if (playerJobStateService.save(newRecord)) {
      Messages.send(
          sender,
          "<primary>✓ Set</primary> <secondary>"
              + targetPlayer.getName()
              + "</secondary><primary>'s experience in</primary> <accent>"
              + job.getPlainName()
              + "</accent> <primary>to</primary> <secondary>"
              + String.format("%.2f", amount)
              + "</secondary>");
      Messages.send(
          targetPlayer,
          "<primary>✓ Your experience in</primary> <accent>"
              + job.getPlainName()
              + "</accent> <primary>has been set to</primary> <secondary>"
              + String.format("%.2f", amount)
              + "</secondary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Messages.send(
          sender, "<error>Failed to update player job state. Please check server logs.</error>");
      return 0;
    }
  }

  /** Adds the supplied experience amount to the selected player's job state. */
  private int executeAdd(
      @NotNull CommandSourceStack source,
      @NotNull PlayerSelectorArgumentResolver playerResolver,
      @NotNull String jobKeyValue,
      double amount)
      throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Messages.send(sender, "<error>Player not found.</error>");
      return 0;
    }

    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Messages.send(
          sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    String playerId = targetPlayer.getUniqueId().toString();
    PlayerJobStateRecord currentRecord =
        playerJobStateService.load(playerId, job.jobKey().asString());

    if (currentRecord == null) {
      Messages.send(
          sender,
          "<secondary>"
              + targetPlayer.getName()
              + "</secondary> <error>is not in job</error> <accent>"
              + job.getPlainName()
              + "</accent>");
      return 0;
    }

    BigDecimal newExperience = currentRecord.experience().add(BigDecimal.valueOf(amount));
    PlayerJobStateRecord newRecord =
        new PlayerJobStateRecord(
            playerId, currentRecord.jobKey(), currentRecord.currentNodeKey(), newExperience);

    if (playerJobStateService.save(newRecord)) {
      Messages.send(
          sender,
          "<primary>✓ Added</primary> <secondary>"
              + String.format("%.2f", amount)
              + " experience</secondary> <primary>to</primary> <accent>"
              + targetPlayer.getName()
              + "</accent> <primary>in</primary> <accent>"
              + job.getPlainName()
              + "</accent> <primary>(total:</primary> <secondary>"
              + String.format("%.2f", newExperience.doubleValue())
              + "</secondary><primary>)</primary>");
      Messages.send(
          targetPlayer,
          "<primary>✓ You gained</primary> <secondary>"
              + String.format("%.2f", amount)
              + " experience</secondary> <primary>in</primary> <accent>"
              + job.getPlainName()
              + "</accent> <primary>(total:</primary> <secondary>"
              + String.format("%.2f", newExperience.doubleValue())
              + "</secondary><primary>)</primary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Messages.send(
          sender, "<error>Failed to update player job state. Please check server logs.</error>");
      return 0;
    }
  }

  /**
   * Subtracts the supplied experience amount from the selected player's job state (floor at zero).
   */
  private int executeSubtract(
      @NotNull CommandSourceStack source,
      @NotNull PlayerSelectorArgumentResolver playerResolver,
      @NotNull String jobKeyValue,
      double amount)
      throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Messages.send(sender, "<error>Player not found.</error>");
      return 0;
    }

    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Messages.send(
          sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    String playerId = targetPlayer.getUniqueId().toString();
    PlayerJobStateRecord currentRecord =
        playerJobStateService.load(playerId, job.jobKey().asString());

    if (currentRecord == null) {
      Messages.send(
          sender,
          "<secondary>"
              + targetPlayer.getName()
              + "</secondary> <error>is not in job</error> <accent>"
              + job.getPlainName()
              + "</accent>");
      return 0;
    }

    BigDecimal newExperience = currentRecord.experience().subtract(BigDecimal.valueOf(amount));
    if (newExperience.compareTo(BigDecimal.ZERO) < 0) {
      newExperience = BigDecimal.ZERO;
    }

    PlayerJobStateRecord newRecord =
        new PlayerJobStateRecord(
            playerId, currentRecord.jobKey(), currentRecord.currentNodeKey(), newExperience);

    if (playerJobStateService.save(newRecord)) {
      Messages.send(
          sender,
          "<primary>✗ Subtracted</primary> <secondary>"
              + String.format("%.2f", amount)
              + " experience</secondary> <primary>from</primary> <accent>"
              + targetPlayer.getName()
              + "</accent> <primary>in</primary> <accent>"
              + job.getPlainName()
              + "</accent> <primary>(total:</primary> <secondary>"
              + String.format("%.2f", newExperience.doubleValue())
              + "</secondary><primary>)</primary>");
      Messages.send(
          targetPlayer,
          "<primary>✗ You lost</primary> <secondary>"
              + String.format("%.2f", amount)
              + " experience</secondary> <primary>in</primary> <accent>"
              + job.getPlainName()
              + "</accent> <primary>(total:</primary> <secondary>"
              + String.format("%.2f", newExperience.doubleValue())
              + "</secondary><primary>)</primary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Messages.send(
          sender, "<error>Failed to update player job state. Please check server logs.</error>");
      return 0;
    }
  }
}
