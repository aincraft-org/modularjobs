package dev.mintychochip.gui;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.gui.JobBrowseModel.Entry;
import dev.mintychochip.gui.JobBrowseModel.Reward;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.service.JoinRefusals;
import dev.mintychochip.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/** Job browse GUI backed by the native Paper inventory host. */
public final class JobBrowseGui {

  private static final int GUI_ROWS = 6;
  private static final String MENU_ID = "job_browse";
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final PaperUiHost host;
  private final JobService jobService;
  private final JoinGate joinGate;
  private final JobBrowseModel model;

  /** Per-audience session: slot index to job key for join dispatch. */
  private final Map<UUID, Map<Integer, String>> sessions = new HashMap<>();

  /** Builds the job-browse presenter over the shared native host. */
  public JobBrowseGui(
      @NotNull PaperUiHost host,
      @NotNull JobService jobService,
      @NotNull JoinGate joinGate,
      @NotNull JobBrowseModel model) {
    this.host = host;
    this.jobService = jobService;
    this.joinGate = joinGate;
    this.model = model;
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
      Job job = jobService.getJob(jobKey);
      List<PlayerJobState> current = jobService.getPlayerJobStates(audience);
      JoinGate.JoinResult result = joinGate.canJoin(player, job, current);
      if (result != JoinGate.JoinResult.ALLOWED) {
        Messages.send(player, JoinRefusals.message(result, job, joinGate, current));
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

    List<Entry> entries = model.entries(player);

    Map<Integer, ItemStack> items = new HashMap<>();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    int slot = 10;
    for (Entry entry : entries) {
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

      items.put(slot, jobItem(entry));
      actions.put(slot, this::onJoin);
      slotJobs.put(slot, entry.job().key().asString());
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

  private @NotNull ItemStack jobItem(@NotNull Entry entry) {
    Job job = entry.job();
    final var state = entry.state();
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
                .append(Component.text(entry.activePlayers(), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false)
                .build()));

    if (entry.upgradeNodes() > 0) {
      lore.add(
          plain(
              Component.text()
                  .append(Component.text("Upgrade Tree: ", NamedTextColor.GRAY))
                  .append(
                      Component.text(entry.upgradeNodes() + " nodes", NamedTextColor.LIGHT_PURPLE))
                  .decoration(TextDecoration.ITALIC, false)
                  .build()));
    }

    lore.add("");
    lore.add(
        plain(
            Component.text("Example Rewards:", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)));
    addExampleRewards(entry, lore);

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

  private static void addExampleRewards(@NotNull Entry entry, @NotNull List<String> lore) {
    if (entry.exampleRewards().isEmpty()) {
      lore.add(
          plain(
              Component.text("  No rewards configured", NamedTextColor.GRAY)
                  .decoration(TextDecoration.ITALIC, false)));
      return;
    }
    for (Reward reward : entry.exampleRewards()) {
      String amount =
          reward.amount().signum() == 0
              ? "0"
              : reward.amount().stripTrailingZeros().toPlainString();
      lore.add(
          plain(
              Component.text()
                  .append(Component.text("  • ", NamedTextColor.DARK_GRAY))
                  .append(
                      Component.text(
                          JobBrowseChatRenderer.humanize(reward.action()), NamedTextColor.YELLOW))
                  .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                  .append(
                      Component.text(
                          amount + " " + JobBrowseChatRenderer.humanize(reward.payableType()),
                          NamedTextColor.GREEN))
                  .decoration(TextDecoration.ITALIC, false)
                  .build()));
    }
  }

  private static @NotNull String plain(@NotNull Component component) {
    return PLAIN.serialize(component);
  }
}
