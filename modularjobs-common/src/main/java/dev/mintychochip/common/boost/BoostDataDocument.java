package dev.mintychochip.common.boost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JSON persistence document for {@code SerializableBoostData}. Rule {@code conditions} are opaque
 * bytes from {@link dev.mintychochip.databag.condition.ConditionSerializer}.
 */
public record BoostDataDocument(
    @NotNull String kind,
    @Nullable String slots,
    @Nullable String duration,
    @NotNull SourceDocument source) {

  private static final @NotNull Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  /** Source document. */
  public record SourceDocument(
      @NotNull String key, @Nullable String description, @NotNull List<RuleDocument> rules) {}

  /** One ruled boost: priority, condition serializer bytes (base64), boost amount. */
  public record RuleDocument(
      int priority, @NotNull String conditions, @NotNull BoostDocument boost) {

    /** Of. */
    @Contract(pure = true)
    public static @NotNull RuleDocument of(
        int priority, @NotNull byte[] conditionBytes, @NotNull BoostDocument boost) {
      return new RuleDocument(priority, Base64.getEncoder().encodeToString(conditionBytes), boost);
    }

    /** Condition bytes. */
    @Contract(pure = true)
    public @NotNull byte[] conditionBytes() {
      return Base64.getDecoder().decode(conditions);
    }
  }

  /** Boost document. */
  public record BoostDocument(@NotNull String type, double amount) {}

  /** Converts to json. */
  @Contract(pure = true)
  public static @NotNull byte[] toJson(@NotNull BoostDataDocument document) {
    return GSON.toJson(document).getBytes(StandardCharsets.UTF_8);
  }

  /** From json. */
  @Contract(pure = true)
  public static @NotNull BoostDataDocument fromJson(@NotNull byte[] bytes) {
    BoostDataDocument document =
        GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), BoostDataDocument.class);
    if (document == null || document.source() == null) {
      throw new IllegalArgumentException("invalid boost data JSON");
    }
    return document;
  }
}
