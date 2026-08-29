package dev.mintychochip.gui;

import dev.mintychochip.Job;
import dev.mintychochip.JobTask;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Payable;
import dev.mintychochip.service.PreferencesService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Job info inventory showing action types and rewards. */
public final class JobInfoGui {

  private static final int GUI_ROWS = 6;
  private static final String MENU_ID = "job_info";
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final PaperUiHost host;
  private final PreferencesService preferencesService;
  private final Map<UUID, Session> sessions = new HashMap<>();

  private record Session(
      Job job, List<Map.Entry<ActionType, List<JobTask>>> entries, int page, int entriesPerPage) {}

  /** Builds the job-info presenter over the shared native host. */
  public JobInfoGui(PaperUiHost host, PreferencesService preferencesService) {
    this.host = host;
    this.preferencesService = preferencesService;
  }

  /** Calculates the number of pages required for the supplied task groups. */
  public int calculateTotalPages(Map<ActionType, List<JobTask>> tasks, int entriesPerPage) {
    return Math.max(1, (int) Math.ceil((double) tasks.size() / Math.max(1, entriesPerPage)));
  }

  /** Opens the job-info inventory when {@code page} is valid. */
  public boolean open(Player player, Job job, Map<ActionType, List<JobTask>> tasks, int page) {
    int entriesPerPage = preferencesService.getEntriesPerPage(player.getUniqueId());
    int totalPages = calculateTotalPages(tasks, entriesPerPage);
    if (page < 1 || page > totalPages) {
      return false;
    }
    List<Map.Entry<ActionType, List<JobTask>>> entries = new ArrayList<>(tasks.entrySet());
    sessions.put(player.getUniqueId(), new Session(job, entries, page, entriesPerPage));
    host.refresh(player, buildView(player.getUniqueId()));
    return true;
  }

  /** Opens the previous page for an owned info screen. */
  public void onPrev(Player player, InventoryClickEvent event) {
    Session session = sessions.get(player.getUniqueId());
    if (session == null || session.page() <= 1) {
      return;
    }
    reopen(player, session.page() - 1);
  }

  /** Opens the next page for an owned info screen. */
  public void onNext(Player player, InventoryClickEvent event) {
    Session session = sessions.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    int total =
        Math.max(
            1,
            (int)
                Math.ceil(
                    (double) session.entries().size() / Math.max(1, session.entriesPerPage())));
    if (session.page() >= total) {
      return;
    }
    reopen(player, session.page() + 1);
  }

  private void reopen(Player player, int page) {
    Session session = sessions.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    Map<ActionType, List<JobTask>> map = new java.util.LinkedHashMap<>();
    for (Map.Entry<ActionType, List<JobTask>> entry : session.entries()) {
      map.put(entry.getKey(), entry.getValue());
    }
    open(player, session.job(), map, page);
  }

  PaperUiHost.ScreenView buildView(UUID audience) {
    Session session = sessions.get(audience);
    if (session == null) {
      return new PaperUiHost.ScreenView(
          MENU_ID,
          GUI_ROWS,
          Component.text("Job Info"),
          panes(),
          Map.of(),
          ignored -> {});
    }

    Job job = session.job();
    int page = session.page();
    int entriesPerPage = session.entriesPerPage();
    List<Map.Entry<ActionType, List<JobTask>>> entries = session.entries();
    int totalPages =
        Math.max(1, (int) Math.ceil((double) entries.size() / Math.max(1, entriesPerPage)));

    String jobName = PLAIN.serialize(job.displayName());
    String title = "Info: " + jobName + " (" + page + "/" + totalPages + ")";
    if (title.length() > 128) {
      title = title.substring(0, 128);
    }

    Map<Integer, ItemStack> items = panes();
    Map<Integer, PaperUiHost.SlotAction> actions = new HashMap<>();
    List<String> headerLore = new ArrayList<>();
    headerLore.add(PLAIN.serialize(job.description()));
    headerLore.add("Max Level: " + job.maxLevel());
    items.put(4, PaperItemFactory.of(Material.BOOK, jobName, headerLore));

    int start = (page - 1) * entriesPerPage;
    int end = Math.min(start + entriesPerPage, entries.size());
    int[] contentSlots = {
      10, 11, 12, 13, 14, 15, 16,
      19, 20, 21, 22, 23, 24, 25,
      28, 29, 30, 31, 32, 33, 34,
      37, 38, 39, 40, 41, 42, 43
    };
    int slotIndex = 0;
    for (int i = start; i < end && slotIndex < contentSlots.length; i++) {
      Map.Entry<ActionType, List<JobTask>> entry = entries.get(i);
      if (entry.getValue().isEmpty()) {
        continue;
      }
      items.put(contentSlots[slotIndex], actionItem(entry.getKey(), entry.getValue()));
      slotIndex++;
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

  private ItemStack actionItem(ActionType type, List<JobTask> tasks) {
    String name = formatActionTypeName(type.name());
    List<String> lore = new ArrayList<>();
    int shown = 0;
    for (JobTask task : tasks) {
      if (shown >= 8) {
        lore.add("… +" + (tasks.size() - shown) + " more");
        break;
      }
      lore.add(formatContextKey(task.contextKey()) + " → " + formatPayables(task.payables()));
      shown++;
    }
    return PaperItemFactory.of(Material.PAPER, name, lore);
  }

  private static String formatPayables(List<Payable> payables) {
    if (payables.isEmpty()) {
      return "No rewards";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < payables.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(PLAIN.serialize(payables.get(i).asComponent()));
    }
    return sb.toString();
  }

  private static String formatActionTypeName(String name) {
    return Arrays.stream(name.toLowerCase(java.util.Locale.ROOT).split("_"))
        .filter(w -> !w.isEmpty())
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  private static String formatContextKey(Key key) {
    String value = key.value();
    return Arrays.stream(value.split("[_/]"))
        .filter(w -> !w.isEmpty())
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }
}
