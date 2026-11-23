package dev.mintychochip.gui;

import dev.mintychochip.gui.JobBrowseModel.Entry;
import dev.mintychochip.util.Messages;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Renders the job catalog with Paper's native Dialog API. */
public final class JobBrowseDialogRenderer {
  private static final int BUTTON_WIDTH = 150;
  private final JobBrowseModel model;

  record ButtonSpec(
      @NotNull Component label, @NotNull Component tooltip, @NotNull String command) {}

  /** Creates a native Paper dialog renderer over the shared browse model. */
  public JobBrowseDialogRenderer(@NotNull JobBrowseModel model) {
    this.model = model;
  }

  /** Opens a dynamic browse dialog for the player. */
  public void open(@NotNull Player player) {
    player.showDialog(build(model.entries(player)));
  }

  @NotNull
  Dialog build(@NotNull List<Entry> entries) {
    Component body = body(entries);
    DialogType type =
        entries.isEmpty() ? DialogType.notice() : DialogType.multiAction(buttons(entries), null, 2);
    return Dialog.create(
        builder ->
            builder
                .empty()
                .base(
                    DialogBase.builder(Messages.component("<primary>Browse Jobs"))
                        .body(List.of(DialogBody.plainMessage(body)))
                        .build())
                .type(type));
  }

  private static @NotNull Component body(@NotNull List<Entry> entries) {
    if (entries.isEmpty()) {
      return Messages.component("<neutral>No jobs are currently available.");
    }
    Component body = Component.empty();
    for (int index = 0; index < entries.size(); index++) {
      Entry entry = entries.get(index);
      if (index > 0) {
        body = body.append(Component.newline()).append(Component.newline());
      }
      body =
          body.append(entry.job().displayName())
              .append(
                  Messages.component(
                      entry.joined() ? " <success>✓ Joined" : " <neutral>· Available"))
              .append(Component.newline())
              .append(entry.job().description())
              .append(Component.newline())
              .append(
                  Messages.component(
                      "<neutral>Max level <secondary>"
                          + entry.job().maxLevel()
                          + " <neutral>· Active <accent>"
                          + entry.activePlayers()));
    }
    return body;
  }

  static @NotNull List<ButtonSpec> buttonSpecs(@NotNull List<Entry> entries) {
    List<ButtonSpec> buttons = new ArrayList<>(entries.size() * 2);
    for (Entry entry : entries) {
      Component tooltip =
          entry
              .job()
              .description()
              .append(
                  Messages.component("\n<neutral>Max level: <secondary>" + entry.job().maxLevel()));
      buttons.add(
          new ButtonSpec(
              Component.text("Details · ").append(entry.job().displayName()),
              tooltip,
              entry.infoCommand()));
      if (!entry.joined()) {
        buttons.add(
            new ButtonSpec(
                Component.text("Join · ").append(entry.job().displayName()),
                tooltip,
                entry.joinCommand()));
      }
    }
    return List.copyOf(buttons);
  }

  private static @NotNull List<ActionButton> buttons(@NotNull List<Entry> entries) {
    return buttonSpecs(entries).stream()
        .map(
            button ->
                ActionButton.create(
                    button.label(),
                    button.tooltip(),
                    BUTTON_WIDTH,
                    DialogAction.staticAction(ClickEvent.runCommand(button.command()))))
        .toList();
  }
}
