package dev.mintychochip.gui;

import dev.mintychochip.JobProgression;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Job statistics inventory view backed by the native Paper inventory host. */
public final class StatsGui {

  private static final int JOBS_PER_PAGE = 5;
  private static final int GUI_ROWS = 6;
  private static final String MENU_ID = "job_stats";
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final PaperUiHost host;
  private final Map<UUID, Session> sessions = new HashMap<>();

  private record Session(OfflinePlayer target, List<JobProgression> progressions, int page) {}

  /** Creates a statistics view renderer backed by the native inventory host. */
  public StatsGui(PaperUiHost host) {
    this.host = host;
  }

  /** Calculates the number of pages required for the supplied progression list. */
  public static int calculateTotalPages(List<JobProgression> progressions) {
    return Math.max(1, (int) Math.ceil((double) progressions.size() / JOBS_PER_PAGE));
  }

  /** Stores the viewer's target and page, then opens the corresponding view. */
  public void open(
      Player viewer, OfflinePlayer target, List<JobProgression> progressions, int page) {
    int totalPages = calculateTotalPages(progressions);
    int safePage = Math.max(1, Math.min(page, totalPages));
    sessions.put(viewer.getUniqueId(), new Session(target, List.copyOf(progressions), safePage));
    host.refresh(viewer, buildView(viewer.getUniqueId()));
  }

  /** Opens the previous page for an owned statistics screen. */
  public void onPrev(Player player, InventoryClickEvent event) {
    Session session = sessions.get(player.getUniqueId());
    if (session == null || session.page() <= 1) {
      return;
    }
    open(player, session.target(), session.progressions(), session.page() - 1);
  }

  /** Opens the next page for an owned statistics screen. */
  public void onNext(Player player, InventoryClickEvent event) {
    Session session = sessions.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    int total = calculateTotalPages(session.progressions());
    if (session.page() >= total) {
      return;
    }
    open(player, session.target(), session.progressions(), session.page() + 1);
  }

  PaperUiHost.ScreenView buildView(UUID audience) {
    Session session = sessions.get(audience);
    if (session == null) {
      return new PaperUiHost.ScreenView(
          MENU_ID,
          GUI_ROWS,
          Component.text("Job Statistics"),
          panes(),
          Map.of(),
          ignored -> {});
    }

    OfflinePlayer target = session.target();
    List<JobProgression> progressions = session.progressions();
    int page = session.page();
    int totalPages = calculateTotalPages(progressions);
    String targetName = target.getName() != null ? target.getName() : "Unknown";

    String title = "Stats: " + targetName + " (" + page + "/" + totalPages + ")";
    if (title.length() > 128) {
      title = title.substring(0, 128);
    }

    Map<Integer, ItemStack> items = panes();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    items.put(
        4,
        PaperItemFactory.of(
            Material.BOOK,
            "Job Statistics",
            List.of("Player: " + targetName, "Jobs: " + progressions.size())));

    int start = (page - 1) * JOBS_PER_PAGE;
    int end = Math.min(start + JOBS_PER_PAGE, progressions.size());
    int[] slots = {19, 20, 21, 22, 23};
    for (int i = start; i < end; i++) {
      JobProgression prog = progressions.get(i);
      int slot = slots[i - start];
      items.put(slot, jobItem(prog));
    }

    if (page > 1) {
      items.put(45, PaperItemFactory.of(Material.ARROW, "Previous", List.of("Page " + (page - 1))));
      actions.put(45, this::onPrev);
    }
    items.put(49, PaperItemFactory.of(Material.PAPER, "Page " + page + "/" + totalPages, List.of()));
    if (page < totalPages) {
      items.put(53, PaperItemFactory.of(Material.ARROW, "Next", List.of("Page " + (page + 1))));
      actions.put(53, this::onNext);
    }

    return new PaperUiHost.ScreenView(
        MENU_ID,
        GUI_ROWS,
        Component.text(title),
        items,
        actions,
        ignored -> sessions.remove(audience));
  }

  private static Map<Integer, ItemStack> panes() {
    Map<Integer, ItemStack> items = new HashMap<>();
    ItemStack pane = PaperItemFactory.pane(Material.GRAY_STAINED_GLASS_PANE);
    for (int i = 0; i < GUI_ROWS * 9; i++) {
      items.put(i, pane);
    }
    return items;
  }

  private ItemStack jobItem(JobProgression prog) {
    int level = prog.level();
    BigDecimal xp = prog.experience();
    List<String> lore = new ArrayList<>();
    lore.add("Level: " + level);
    lore.add("XP: " + xp.setScale(1, RoundingMode.HALF_UP).toPlainString());
    try {
      BigDecimal next =
          prog.job()
              .levelingCurve()
              .evaluate(new dev.mintychochip.LevelingCurve.Parameters(level + 1));
      lore.add("Next level: " + next.setScale(1, RoundingMode.HALF_UP).toPlainString());
    } catch (IllegalArgumentException | ArithmeticException ignored) {
      // The curve may not support level+1.
    }
    String name = PLAIN.serialize(prog.job().displayName());
    return PaperItemFactory.of(Material.EMERALD, name, lore);
  }
}
