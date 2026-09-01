package dev.mintychochip.gui;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Payable;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.upgrade.UpgradeService;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.util.Messages;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Job browse GUI backed by the native Paper inventory host. */
public final class JobBrowseGui {

  private static final int GUI_ROWS = 6;
  private static final String MENU_ID = "job_browse";
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final PaperUiHost host;
  private final JobService jobService;
  private final UpgradeService upgradeService;
  private final JoinGate joinGate;

  /** Per-audience session: slot index to job key for join dispatch. */
  private final Map<UUID, Map<Integer, String>> sessions = new HashMap<>();

  /** Builds the job-browse presenter over the shared native host. */
  public JobBrowseGui(
      @NotNull PaperUiHost host,
      @NotNull JobService jobService,
      @NotNull UpgradeService upgradeService,
      @NotNull JoinGate joinGate) {
    this.host = host;
    this.jobService = jobService;
    this.upgradeService = upgradeService;
    this.joinGate = joinGate;
  }

  /** Opens the browse menu for {@code player}. */
  public void open(@NotNull Player player) {
    host.refresh(player, buildView(player));
  }

  /** Host action: join the job mapped to the clicked slot. */
  public void onJoin(@NotNull Player player, @NotNull InventoryClickEvent event) {
    UUID audience = player.getUniqueId();
    Map<Integer, String> slotJobs = sessions.get(audience);
    if (slotJobs == null) {
      return;
    }
    String jobKey = slotJobs.get(event.getRawSlot());
    if (jobKey == null) {
      return;
    }

    String playerId = audience.toString();
    String name;
    try {
      name = jobService.getJob(jobKey).getPlainName();
    } catch (IllegalArgumentException e) {
      Messages.send(player, "<error>Job not found: " + jobKey);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      return;
    }
    try {
      if (jobService.getPlayerJobState(playerId, jobKey) != null) {
        Messages.send(
            player, "<neutral>You are already in</neutral> <secondary>" + name + "</secondary>.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
        return;
      }
      JoinGate.JoinResult result =
          joinGate.canJoin(
              player, jobService.getJob(jobKey), jobService.getPlayerJobStates(audience));
      if (result != JoinGate.JoinResult.ALLOWED) {
        Messages.send(
            player,
            switch (result) {
              case MAX_JOBS -> "<error>You reached the maximum number of jobs you can join.";
              case PERMISSION_DENIED ->
                  "<error>You do not have permission to join</error> <secondary>"
                      + name
                      + "</secondary><error>.</error>";
              case WORLD_DENIED -> "<error>You cannot join jobs while in this world.";
              case ALLOWED -> "";
            });
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
        return;
      }
      if (jobService.joinJob(playerId, jobKey)) {
        Messages.send(
            player,
            "<primary>✓ You joined</primary> <secondary>"
                + name
                + "</secondary> <primary>!</primary>");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        host.close(player);
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(JobBrowseGui.class);
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  if (player.isOnline()) {
                    open(player);
                  }
                },
                1L);
      } else {
        Messages.send(
            player, "<neutral>You could not join</neutral> <secondary>" + name + "</secondary>.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
      }
    } catch (IllegalArgumentException e) {
      Messages.send(player, "<error>Job not found: " + jobKey);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
  }

  @NotNull
  PaperUiHost.ScreenView buildView(@NotNull Player player) {
    UUID audience = player.getUniqueId();
    Map<Integer, String> slotJobs = new HashMap<>();

    List<Job> allJobs = jobService.getJobs();
    List<PlayerJobState> playerJobs = jobService.getPlayerJobStates(audience);
    Map<String, PlayerJobState> playerJobMap = new HashMap<>();
    for (PlayerJobState prog : playerJobs) {
      playerJobMap.put(prog.job().key().asString(), prog);
    }

    Map<Integer, ItemStack> items = new HashMap<>();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    int slot = 10;
    for (Job job : allJobs) {
      int row = slot / 9;
      int col = slot % 9;
      if (row >= 5) {
        break;
      }
      if (col == 8) {
        row++;
        col = 1;
        slot = row * 9 + col;
      }
      if (col == 0) {
        col = 1;
        slot = row * 9 + col;
      }
      if (row >= 5 || slot >= GUI_ROWS * 9) {
        break;
      }

      PlayerJobState state = playerJobMap.get(job.key().asString());
      items.put(slot, jobItem(job, state));
      actions.put(slot, this::onJoin);
      slotJobs.put(slot, job.key().asString());
      slot++;
    }

    ItemStack pane = PaperItemFactory.pane(Material.GRAY_STAINED_GLASS_PANE);
    for (int i = 0; i < GUI_ROWS * 9; i++) {
      items.putIfAbsent(i, pane);
    }

    sessions.put(audience, Map.copyOf(slotJobs));
    return new PaperUiHost.ScreenView(
        MENU_ID,
        GUI_ROWS,
        Component.text("Browse Jobs"),
        items,
        actions,
        ignored -> sessions.remove(audience));
  }

  private @NotNull ItemStack jobItem(@NotNull Job job, @Nullable PlayerJobState state) {
    List<String> lore = new ArrayList<>();
    lore.add(
        plain(
            job.description().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
    lore.add("");
    lore.add(
        plain(
            Component.text()
                .append(Component.text("Max Level: ", NamedTextColor.GRAY))
                .append(Component.text(job.maxLevel(), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false)
                .build()));
    lore.add(
        plain(
            Component.text()
                .append(Component.text("Active Players: ", NamedTextColor.GRAY))
                .append(Component.text(countActivePlayers(job), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false)
                .build()));

    Optional<UpgradeTree> treeOpt = upgradeService.getTree(job.key().value());
    if (treeOpt.isPresent()) {
      UpgradeTree tree = treeOpt.get();
      lore.add(
          plain(
              Component.text()
                  .append(Component.text("Upgrade Tree: ", NamedTextColor.GRAY))
                  .append(
                      Component.text(
                          tree.allNodes().size() + " nodes", NamedTextColor.LIGHT_PURPLE))
                  .decoration(TextDecoration.ITALIC, false)
                  .build()));
    }

    lore.add("");
    lore.add(
        plain(
            Component.text("Example Rewards:", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)));
    addExampleRewards(job, lore);

    boolean isJoined = state != null;
    if (isJoined) {
      lore.add("");
      lore.add(
          plain(
              Component.text()
                  .append(Component.text("Your Level: ", NamedTextColor.GRAY))
                  .append(Component.text(state.level(), NamedTextColor.GREEN))
                  .decoration(TextDecoration.ITALIC, false)
                  .build()));
      lore.add(
          plain(
              Component.text()
                  .append(Component.text("Experience: ", NamedTextColor.GRAY))
                  .append(Component.text(state.experience().toPlainString(), NamedTextColor.AQUA))
                  .decoration(TextDecoration.ITALIC, false)
                  .build()));
      lore.add("");
      lore.add(
          plain(
              Component.text("✓ Already Joined", NamedTextColor.GREEN)
                  .decoration(TextDecoration.ITALIC, false)));
    } else {
      lore.add("");
      lore.add(
          plain(
              Component.text("Click to join!", NamedTextColor.YELLOW)
                  .decoration(TextDecoration.ITALIC, false)));
    }

    Material material = isJoined ? Material.EMERALD : Material.BOOK;
    NamedTextColor nameColor = isJoined ? NamedTextColor.GREEN : NamedTextColor.GOLD;
    String name =
        PLAIN.serialize(
            job.displayName().color(nameColor).decoration(TextDecoration.ITALIC, false));
    return PaperItemFactory.of(material, name, lore);
  }

  private int countActivePlayers(@NotNull Job job) {
    int count = 0;
    String jobKey = job.key().asString();
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      for (PlayerJobState prog : jobService.getPlayerJobStates(onlinePlayer.getUniqueId())) {
        if (prog.job().key().asString().equals(jobKey)) {
          count++;
          break;
        }
      }
    }
    return count;
  }

  private void addExampleRewards(@NotNull Job job, @NotNull List<String> lore) {
    Map<ActionType, List<dev.mintychochip.JobTask>> allTasks =
        jobService.getAllTasks(job, job.rootNode().nodeKey());
    int examplesAdded = 0;
    int maxExamples = 3;

    for (Map.Entry<ActionType, List<dev.mintychochip.JobTask>> entry : allTasks.entrySet()) {
      if (examplesAdded >= maxExamples) {
        break;
      }
      List<dev.mintychochip.JobTask> tasks = entry.getValue();
      if (tasks.isEmpty()) {
        continue;
      }
      dev.mintychochip.JobTask exampleTask = tasks.get(0);
      List<Payable> payables = exampleTask.payables();
      if (payables.isEmpty()) {
        continue;
      }
      Payable payable = payables.get(0);
      BigDecimal amount = payable.amount().value();
      String payableTypeName = payable.type().key().value();
      String actionName = entry.getKey().key().value();
      if (actionName.contains(":")) {
        actionName = actionName.substring(actionName.indexOf(':') + 1);
      }
      String formattedPayable = formatPayableType(payableTypeName);
      lore.add(
          plain(
              Component.text()
                  .append(Component.text("  • ", NamedTextColor.DARK_GRAY))
                  .append(Component.text(actionName, NamedTextColor.YELLOW))
                  .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                  .append(
                      Component.text(
                          amount.toPlainString() + " " + formattedPayable, NamedTextColor.GREEN))
                  .decoration(TextDecoration.ITALIC, false)
                  .build()));
      examplesAdded++;
    }
    if (examplesAdded == 0) {
      lore.add(
          plain(
              Component.text("  No rewards configured", NamedTextColor.GRAY)
                  .decoration(TextDecoration.ITALIC, false)));
    }
  }

  private static @NotNull String formatPayableType(@NotNull String payableType) {
    if (payableType.contains(":")) {
      payableType = payableType.substring(payableType.indexOf(':') + 1);
    }
    String[] parts = payableType.split("_");
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (result.length() > 0) {
        result.append(' ');
      }
      result.append(part.substring(0, 1).toUpperCase());
      if (part.length() > 1) {
        result.append(part.substring(1).toLowerCase());
      }
    }
    return result.toString();
  }

  private static @NotNull String plain(@NotNull Component component) {
    return PLAIN.serialize(component);
  }
}
