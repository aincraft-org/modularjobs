package dev.mintychochip.databag.condition;

import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Immutable block identity and state properties, excluding location data. */
public record BlockSnapshot(Key blockId, Map<String, String> properties)
    implements SubjectSnapshot {

  public BlockSnapshot {
    Objects.requireNonNull(blockId, "blockId");
    properties = Map.copyOf(properties == null ? Map.of() : properties);
  }
}
