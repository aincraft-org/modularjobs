package dev.mintychochip.registry;

import dev.mintychochip.action.ActionTypeImpl;
import dev.mintychochip.container.ActionType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Builds the built-in action-type registry for the plugin namespace. */
public final class ActionTypeRegistryProvider {

  private final Plugin plugin;

  /** Creates a provider bound to the plugin namespace used for action keys. */
  public ActionTypeRegistryProvider(@NotNull Plugin plugin) {
    this.plugin = plugin;
  }

  /** Creates and populates the built-in action-type registry. */
  public static @NotNull Registry<ActionType> create(@NotNull Plugin plugin) {
    return new ActionTypeRegistryProvider(plugin).get();
  }

  @NotNull
  Registry<ActionType> get() {
    SimpleRegistryImpl<ActionType> r = new SimpleRegistryImpl<>();
    r.register(actionType("block_place"));
    r.register(actionType("block_break"));
    r.register(actionType("tnt_break"));
    r.register(actionType("kill"));
    r.register(actionType("dye"));
    r.register(actionType("strip_log"));
    r.register(actionType("craft"));
    r.register(actionType("fish"));
    r.register(actionType("smelt"));
    r.register(actionType("brew"));
    r.register(actionType("enchant"));
    r.register(actionType("repair"));
    r.register(actionType("breed"));
    r.register(actionType("tame"));
    r.register(actionType("shear"));
    r.register(actionType("milk"));
    r.register(actionType("explore"));
    r.register(actionType("eat"));
    r.register(actionType("collect"));
    r.register(actionType("bake"));
    r.register(actionType("bucket"));
    r.register(actionType("brush"));
    r.register(actionType("wax"));
    r.register(actionType("villager_trade"));
    return r;
  }

  private @NotNull ActionType actionType(@NotNull String name, @NotNull String keyString) {
    return new ActionTypeImpl(name, new NamespacedKey(plugin, keyString));
  }

  private @NotNull ActionType actionType(@NotNull String keyString) {
    StringBuilder sb = new StringBuilder(keyString.length());
    boolean capitalNext = true;
    for (char c : keyString.toCharArray()) {
      if (c == '_') {
        sb.append(' ');
        capitalNext = true;
      } else {
        sb.append(capitalNext ? Character.toUpperCase(c) : Character.toLowerCase(c));
        capitalNext = false;
      }
    }
    return actionType(sb.toString(), keyString);
  }
}
