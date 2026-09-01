package dev.mintychochip.profession;

import java.io.InputStream;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Minimal plugin used to exercise {@code saveResource} for profession and recipe startup tests. */
public class ProfessionWiringTestPlugin extends JavaPlugin {

  @Override
  public void onEnable() {}

  @Override
  public @Nullable InputStream getResource(@NotNull String filename) {
    InputStream fromClasspath =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    if (fromClasspath != null) {
      return fromClasspath;
    }
    return super.getResource(filename);
  }
}
