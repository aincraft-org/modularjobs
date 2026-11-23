package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.commands.BrowseCommand;
import dev.mintychochip.gui.JobBrowseDialogRenderer.ButtonSpec;
import dev.mintychochip.gui.JobBrowseModel.Entry;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class JobBrowseRendererTest {

  @Test
  void commandExposesGuiChatAndDialogModes() {
    BrowseCommand command =
        new BrowseCommand(
            new JobBrowseGui(null, null, null, null),
            new JobBrowseChatRenderer(null),
            new JobBrowseDialogRenderer(null));
    var root = command.build().build();

    assertNotNull(root.getChild("gui"));
    assertNotNull(root.getChild("chat"));
    assertNotNull(root.getChild("dialog"));
  }

  @Test
  void chatEntryExposesSharedInfoAndJoinCommandTargets() {
    Entry entry = entry(false);
    List<Component> lines = new JobBrowseChatRenderer(null).render(List.of(entry));
    Set<ClickEvent> actions = new HashSet<>();
    lines.forEach(line -> collectClicks(line, actions));

    assertTrue(actions.contains(ClickEvent.runCommand("/j info modularjobs:miner")));
    assertTrue(actions.contains(ClickEvent.runCommand("/j join modularjobs:miner")));
  }

  @Test
  void dialogButtonsExposeSharedInfoAndJoinCommandTargets() {
    List<ButtonSpec> buttons = JobBrowseDialogRenderer.buttonSpecs(List.of(entry(false)));

    assertEquals(2, buttons.size());
    assertEquals("/j info modularjobs:miner", buttons.get(0).command());
    assertEquals("/j join modularjobs:miner", buttons.get(1).command());
  }

  @Test
  void joinedEntriesDoNotOfferAnotherJoinAction() {
    Entry entry = entry(true);

    List<ButtonSpec> buttons = JobBrowseDialogRenderer.buttonSpecs(List.of(entry));
    assertEquals(1, buttons.size());

    Set<ClickEvent> chatActions = new HashSet<>();
    new JobBrowseChatRenderer(null)
        .render(List.of(entry))
        .forEach(line -> collectClicks(line, chatActions));
    assertTrue(chatActions.contains(ClickEvent.runCommand("/j info modularjobs:miner")));
    assertFalse(chatActions.contains(ClickEvent.runCommand("/j join modularjobs:miner")));
  }

  private static @NotNull Entry entry(boolean joined) {
    return new Entry(job(), joined ? state() : null, 3, 4, List.of());
  }

  private static @NotNull Job job() {
    Key key = Key.key("modularjobs", "miner");
    return (Job)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {Job.class},
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "jobKey" -> new JobKey(key);
                  case "key" -> key;
                  case "displayName" -> Component.text("Miner");
                  case "description" -> Component.text("Mine valuable ores");
                  case "getPlainName" -> "Miner";
                  case "maxLevel" -> 100;
                  default -> null;
                });
  }

  private static @NotNull PlayerJobState state() {
    return (PlayerJobState)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {PlayerJobState.class},
            (proxy, method, args) -> null);
  }

  private static void collectClicks(
      @NotNull Component component, @NotNull Set<ClickEvent> actions) {
    if (component.clickEvent() != null) {
      actions.add(component.clickEvent());
    }
    component.children().forEach(child -> collectClicks(child, actions));
  }
}
