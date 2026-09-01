package dev.mintychochip.domain;

import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.util.KeyUtils;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Immutable node definition within a job tree. */
record JobNodeImpl(
    @NotNull JobKey jobKey,
    @NotNull JobNodeKey nodeKey,
    @Nullable JobNodeKey parentKey,
    @NotNull Component displayName,
    @NotNull Component description)
    implements JobNode {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  JobNodeImpl {
    Objects.requireNonNull(jobKey, "jobKey");
    Objects.requireNonNull(nodeKey, "nodeKey");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(description, "description");
  }

  @Override
  public @NotNull String getPlainName() {
    return PlainTextComponentSerializer.plainText().serialize(displayName);
  }

  static @NotNull JobNodeImpl fromRecord(
      @NotNull JobKey jobKey, @NotNull JobRecord record, @NotNull Plugin plugin) {
    return new JobNodeImpl(
        jobKey,
        new JobNodeKey(KeyUtils.parseKey(plugin, record.jobKey())),
        record.parentKey() == null
            ? null
            : new JobNodeKey(KeyUtils.parseKey(plugin, record.parentKey())),
        MINI_MESSAGE.deserialize(record.displayName()),
        record.description() == null
            ? Component.empty()
            : MINI_MESSAGE.deserialize(record.description()));
  }
}
