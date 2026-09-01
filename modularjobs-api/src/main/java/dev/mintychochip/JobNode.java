package dev.mintychochip;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Immutable definition of one node within a {@link Job} tree. */
public interface JobNode extends Keyed {

  /** Returns the owning job tree's identity. */
  @Contract(pure = true)
  @NotNull
  JobKey jobKey();

  /** Returns this node's typed identity. */
  @Contract(pure = true)
  @NotNull
  JobNodeKey nodeKey();

  /** Returns the underlying Adventure key for registry interoperability. */
  @Override
  @Contract(pure = true)
  default @NotNull Key key() {
    return nodeKey().key();
  }

  /** Returns the direct parent node key, or {@code null} for the root node. */
  @Contract(pure = true)
  @Nullable
  JobNodeKey parentKey();

  /** Returns the component displayed as this node's name. */
  @Contract(pure = true)
  @NotNull
  Component displayName();

  /** Returns this node's plain-text name. */
  @Contract(pure = true)
  @NotNull
  String getPlainName();

  /** Returns the component describing this node. */
  @Contract(pure = true)
  @NotNull
  Component description();
}
