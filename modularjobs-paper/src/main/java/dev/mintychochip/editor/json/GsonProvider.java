package dev.mintychochip.editor.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Instant;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory that creates a configured Gson instance for the ModularJobs web editor.
 *
 * <p>Configuration:
 *
 * <ul>
 *   <li>Pretty printing enabled for readability
 *   <li>Lenient parsing enabled
 *   <li>Null values are NOT serialized (default behavior)
 * </ul>
 *
 * <p>Custom type adapters:
 *
 * <ul>
 *   <li>{@link Key} - serialized as "namespace:value" strings
 *   <li>{@link Instant} - serialized as ISO-8601 strings
 * </ul>
 */
public final class GsonProvider {

  private GsonProvider() {}

  /** Create. */
  @Contract(pure = true)
  public static @NotNull Gson create() {
    return new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Key.class, new KeyAdapter())
        .registerTypeAdapter(Instant.class, new InstantAdapter())
        .setLenient()
        .create();
  }

  /**
   * TypeAdapter for Adventure Key serialization.
   *
   * <p>Writes keys as "namespace:value" strings and reads them using {@link Key#key(String)}.
   */
  private static final class KeyAdapter extends TypeAdapter<Key> {

    @Override
    public void write(@NotNull JsonWriter out, @Nullable Key value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.namespace() + ":" + value.value());
    }

    @Override
    public @NotNull Key read(@NotNull JsonReader in) throws IOException {
      String keyString = in.nextString();
      return Key.key(keyString);
    }
  }

  /**
   * TypeAdapter for Instant serialization.
   *
   * <p>Writes instants as ISO-8601 strings and reads them using {@link
   * Instant#parse(CharSequence)}.
   */
  private static final class InstantAdapter extends TypeAdapter<Instant> {

    @Override
    public void write(@NotNull JsonWriter out, @Nullable Instant value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.toString());
    }

    @Override
    public @NotNull Instant read(@NotNull JsonReader in) throws IOException {
      String instantString = in.nextString();
      return Instant.parse(instantString);
    }
  }
}
