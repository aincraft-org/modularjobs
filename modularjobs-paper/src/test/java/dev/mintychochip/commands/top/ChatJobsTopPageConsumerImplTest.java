package dev.mintychochip.commands.top;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.commands.Page;
import dev.mintychochip.test.MockBukkitSupport;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class ChatJobsTopPageConsumerImplTest {
  private static ServerMock server;

  @BeforeAll
  static void setUp() {
    server = MockBukkitSupport.mockServer();
  }

  @AfterAll
  static void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void navigationUsesJobKeyWhenDisplayNameContainsSpaces() {
    PlayerMock sender = server.addPlayer("top-" + UUID.randomUUID());
    Job job = job("Mining Expert");

    new ChatJobsTopPageConsumerImpl()
        .consume(job, new Page<>(List.of(), 1, 10), sender, 2, List.of());

    Set<ClickEvent> actions = new HashSet<>();
    Component message = sender.nextComponentMessage();
    while (message != null) {
      collectClicks(message, actions);
      message = sender.nextComponentMessage();
    }
    assertTrue(actions.contains(ClickEvent.runCommand("/jobs top modularjobs:miner 2")));
    assertFalse(actions.contains(ClickEvent.runCommand("/jobs top Mining Expert 2")));
  }

  private static @NotNull Job job(@NotNull String displayName) {
    JobKey key = new JobKey(Key.key("modularjobs", "miner"));
    return (Job)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {Job.class},
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "jobKey" -> key;
                  case "key" -> key.key();
                  case "displayName" -> Component.text(displayName);
                  case "toString" -> key.asString();
                  case "hashCode" -> key.hashCode();
                  default -> throw new UnsupportedOperationException(method.getName());
                });
  }

  private static void collectClicks(
      @NotNull Component component, @NotNull Set<ClickEvent> actions) {
    if (component.clickEvent() != null) {
      actions.add(component.clickEvent());
    }
    component.children().forEach(child -> collectClicks(child, actions));
  }
}
