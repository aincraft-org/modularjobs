package dev.mintychochip.commands;

import dev.mintychochip.gui.PaperSurfaces;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Ephemeral sidebar scoreboard backed by native Paper surfaces. */
public final class TextScoreboard {

  private static final int MAX_LINES = 15;
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final PaperSurfaces surfaces;
  private final String title;
  private final String[] lines = new String[MAX_LINES];

  private TextScoreboard(@NotNull PaperSurfaces surfaces, @NotNull String title) {
    this.surfaces = surfaces;
    this.title = title;
  }

  /** Creates a text scoreboard for the supplied display name. */
  @Contract(pure = true)
  public static @NotNull TextScoreboard create(
      @NotNull PaperSurfaces surfaces, @NotNull Component displayName) {
    return new TextScoreboard(surfaces, PLAIN.serialize(displayName));
  }

  /** Sets the line. */
  public void setLine(int index, @Nullable ComponentLike prefix, @Nullable ComponentLike suffix) {
    if (index < 0 || index >= MAX_LINES) {
      throw new IndexOutOfBoundsException("scoreboard line " + index);
    }
    String left = prefix == null ? "" : PLAIN.serialize(prefix.asComponent());
    String right = suffix == null ? "" : PLAIN.serialize(suffix.asComponent());
    lines[index] = left + right;
  }

  /** Show. */
  public void show(@NotNull Player player, @NotNull Duration duration) {
    setCurrent(player);
  }

  /** Sets the current. */
  public void setCurrent(@Nullable Player player) {
    if (player == null) {
      return;
    }
    List<String> body = new ArrayList<>(MAX_LINES);
    for (String line : lines) {
      if (line != null) {
        body.add(line);
      }
    }
    surfaces.showScoreboard(player.getUniqueId(), title, body);
  }
}
