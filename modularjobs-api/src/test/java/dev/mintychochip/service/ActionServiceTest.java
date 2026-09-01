package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class ActionServiceTest {

  @Test
  void rawKeyReportDelegatesAsKeyedContext() {
    UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    ActionType action = new TestActionType("Quest Complete", Key.key("myplugin", "quest_complete"));
    Key contextKey = Key.key("myplugin", "first_quest");
    RecordingActionService service = new RecordingActionService();

    service.report(playerId, action, contextKey);

    assertEquals(playerId, service.playerId);
    assertSame(action, service.action);
    assertEquals(new Context.KeyContext(contextKey), service.context);
  }

  @Test
  void rawKeyReportNamesNullContextKey() {
    UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    ActionType action = new TestActionType("Quest Complete", Key.key("myplugin", "quest_complete"));
    RecordingActionService service = new RecordingActionService();

    NullPointerException failure =
        assertThrows(
            NullPointerException.class, () -> service.report(playerId, action, (Key) null));

    assertEquals("contextKey", failure.getMessage());
  }

  private record TestActionType(@NotNull String name, @NotNull Key key) implements ActionType {}

  private static final class RecordingActionService implements ActionService {
    private UUID playerId;
    private ActionType action;
    private Context context;

    @Override
    public @NotNull ActionType register(@NotNull Key key, @NotNull String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void report(@NotNull UUID playerId, @NotNull ActionType type, @NotNull Context context) {
      this.playerId = playerId;
      this.action = type;
      this.context = context;
    }
  }
}
