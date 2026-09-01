package dev.mintychochip.profession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ProfessionIndex {

  private final List<ProfessionDefinition> tracks;
  private final Map<String, ProfessionDefinition> byId;
  private final Map<String, ProfessionDefinition> byStorageKey;

  ProfessionIndex(@NotNull List<ProfessionDefinition> definitions) {
    Objects.requireNonNull(definitions, "definitions");
    if (definitions.isEmpty()) {
      throw new IllegalArgumentException("profession definitions must not be empty");
    }
    tracks = List.copyOf(definitions);

    Map<String, ProfessionDefinition> ids = new LinkedHashMap<>();
    Map<String, ProfessionDefinition> storageKeys = new LinkedHashMap<>();
    Map<String, Integer> idIndexes = new LinkedHashMap<>();
    Map<String, Integer> storageIndexes = new LinkedHashMap<>();
    for (int index = 0; index < tracks.size(); index++) {
      ProfessionDefinition definition = Objects.requireNonNull(tracks.get(index), "definition");
      putUnique(ids, idIndexes, definition.id(), definition, index, "id");
      putUnique(
          storageKeys,
          storageIndexes,
          definition.storageKey(),
          definition,
          index,
          "storage-key");
    }
    for (Map.Entry<String, Integer> entry : idIndexes.entrySet()) {
      Integer storageIndex = storageIndexes.get(entry.getKey());
      if (storageIndex != null && !storageIndex.equals(entry.getValue())) {
        throw new IllegalArgumentException(
            "profession lookup key '"
                + entry.getKey()
                + "' is id at index "
                + entry.getValue()
                + " and storage-key at index "
                + storageIndex);
      }
    }
    byId = Map.copyOf(ids);
    byStorageKey = Map.copyOf(storageKeys);
  }

  @NotNull List<ProfessionDefinition> tracks() {
    return tracks;
  }

  @NotNull Optional<ProfessionDefinition> resolve(@Nullable String input) {
    if (input == null || input.isBlank()) {
      return Optional.empty();
    }
    String key = input.trim().toLowerCase(Locale.ROOT);
    int colon = key.indexOf(':');
    if (colon >= 0) {
      key = key.substring(colon + 1);
    }
    ProfessionDefinition definition = byId.get(key);
    return Optional.ofNullable(definition != null ? definition : byStorageKey.get(key));
  }

  private static void putUnique(
      @NotNull Map<String, ProfessionDefinition> definitions,
      @NotNull Map<String, Integer> indexes,
      @NotNull String key,
      @NotNull ProfessionDefinition definition,
      int index,
      @NotNull String field) {
    Integer previous = indexes.putIfAbsent(key, index);
    if (previous != null) {
      throw new IllegalArgumentException(
          "duplicate profession " + field + " '" + key + "' at indexes " + previous + " and " + index);
    }
    definitions.put(key, definition);
  }
}
