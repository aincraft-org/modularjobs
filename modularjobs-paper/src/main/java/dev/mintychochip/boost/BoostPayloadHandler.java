package dev.mintychochip.boost;

import dev.mintychochip.databag.DataHandler;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** DataBag codec for item boost JSON ({@code modularjobs:boost_data}, format 1). */
public final class BoostPayloadHandler implements DataHandler<byte[]> {

  public static final Key KEY = Key.key("modularjobs", "boost_data");
  public static final int FORMAT = 1;
  public static final BoostPayloadHandler INSTANCE = new BoostPayloadHandler();

  private BoostPayloadHandler() {}

  @Override
  @Contract(pure = true)
  public @NotNull Key key() {
    return KEY;
  }

  @Override
  @Contract(pure = true)
  public int format() {
    return FORMAT;
  }

  @Override
  @Contract(pure = true)
  public @NotNull byte[] encode(@NotNull byte[] value) {
    return value.clone();
  }

  @Override
  @Contract(pure = true)
  public @NotNull byte[] decode(@NotNull byte[] bytes) {
    return bytes.clone();
  }
}
