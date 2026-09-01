package dev.mintychochip.upgrade.rendering.editor;

import dev.mintychochip.upgrade.UpgradeEffect;
import dev.mintychochip.upgrade.UpgradeNode;
import dev.mintychochip.upgrade.rendering.Position;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Mutable upgrade node for editing purposes. */
public final class EditorNode {

  private String id;
  private String name;
  private String description;
  private Material icon;
  private Material unlockedIcon;
  private String itemModel;
  private String unlockedItemModel;
  private int cost;
  private Position position;
  private String archetypeRef;
  private String perkId;
  private int level;

  private final Set<String> prerequisites = new HashSet<>();
  private final Set<String> exclusive = new HashSet<>();
  private final List<String> children = new ArrayList<>();
  private final List<EditorEffect> effects = new ArrayList<>();

  /** Editor node. */
  public EditorNode() {
    this.id = "";
    this.name = "New Node";
    this.description = "";
    this.icon = Material.STONE;
    this.unlockedIcon = Material.STONE;
    this.itemModel = null;
    this.unlockedItemModel = null;
    this.cost = 1;
    this.position = new Position(0, 0);
    this.archetypeRef = null;
    this.perkId = "";
    this.level = 1;
  }

  /** Create from an existing UpgradeNode. */
  @Contract(pure = true)
  public static @NotNull EditorNode fromUpgradeNode(@NotNull UpgradeNode source) {
    EditorNode node = new EditorNode();

    // Extract short key from namespaced key
    String fullKey = source.key().asString();
    int colonIndex = fullKey.indexOf(':');
    node.id = colonIndex >= 0 ? fullKey.substring(colonIndex + 1) : fullKey;

    node.name = source.name();
    node.description = source.description();
    node.icon = resolveMaterial(source.icon());
    node.unlockedIcon = resolveMaterial(source.unlockedIcon());
    node.itemModel = source.itemModel();
    node.unlockedItemModel = source.unlockedItemModel();
    node.cost = source.cost();
    node.position = source.position();
    node.perkId = source.perkId();
    node.level = source.level();

    node.prerequisites.addAll(source.prerequisites());
    node.exclusive.addAll(source.exclusive());
    node.children.addAll(source.children());

    // Convert effects
    for (UpgradeEffect effect : source.effects()) {
      node.effects.add(EditorEffect.fromUpgradeEffect(effect));
    }

    return node;
  }

  // ========== Getters/Setters ==========

  /** Id. */
  @Contract(pure = true)
  public @Nullable String id() {
    return id;
  }

  public void setId(@Nullable String id) {
    this.id = id;
  }

  /** Name. */
  @Contract(pure = true)
  public @Nullable String name() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  /** Description. */
  @Contract(pure = true)
  public @Nullable String description() {
    return description;
  }

  /** Sets the description. */
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  /** Icon. */
  @Contract(pure = true)
  public @Nullable Material icon() {
    return icon;
  }

  /** API member. */
  public void setIcon(@Nullable Material icon) {
    this.icon = icon;
  }

  /** Unlocked icon. */
  @Contract(pure = true)
  public @Nullable Material unlockedIcon() {
    return unlockedIcon;
  }

  public void setUnlockedIcon(@Nullable Material icon) {
    this.unlockedIcon = icon;
  }

  /** Item model. */
  @Contract(pure = true)
  public @Nullable String itemModel() {
    return itemModel;
  }

  public void setItemModel(@Nullable String itemModel) {
    this.itemModel = itemModel;
  }

  /** Unlocked item model. */
  @Contract(pure = true)
  public @Nullable String unlockedItemModel() {
    return unlockedItemModel;
  }

  public void setUnlockedItemModel(@Nullable String unlockedItemModel) {
    this.unlockedItemModel = unlockedItemModel;
  }

  /** Cost. */
  @Contract(pure = true)
  public int cost() {
    return cost;
  }

  public void setCost(int cost) {
    this.cost = Math.max(0, cost);
  }

  /** Position. */
  @Contract(pure = true)
  public @Nullable Position position() {
    return position;
  }

  public void setPosition(@Nullable Position position) {
    this.position = position;
  }

  /** Archetype ref. */
  @Contract(pure = true)
  public @Nullable String archetypeRef() {
    return archetypeRef;
  }

  public void setArchetypeRef(@Nullable String archetypeRef) {
    this.archetypeRef = archetypeRef;
  }

  /** Perk id. */
  @Contract(pure = true)
  public @Nullable String perkId() {
    return perkId;
  }

  /** Sets the perk id. */
  public void setPerkId(@Nullable String perkId) {
    this.perkId = perkId;
  }

  /** Level. */
  @Contract(pure = true)
  public int level() {
    return level;
  }

  /** API member. */
  public void setLevel(int level) {
    this.level = Math.max(1, level);
  }

  /** Prerequisites. */
  @Contract(pure = true)
  public @NotNull Set<String> prerequisites() {
    return prerequisites;
  }

  /** Exclusive. */
  @Contract(pure = true)
  public @NotNull Set<String> exclusive() {
    return exclusive;
  }

  /** Children. */
  @Contract(pure = true)
  public @NotNull List<String> children() {
    return children;
  }

  /** Effects. */
  @Contract(pure = true)
  public @NotNull List<EditorEffect> effects() {
    return effects;
  }

  // ========== Helpers ==========

  @Contract(value = "null -> !null", pure = true)
  private static @NotNull Material resolveMaterial(@Nullable String name) {
    if (name == null || name.isBlank()) {
      return Material.BARRIER;
    }
    Material material = Material.matchMaterial(name);
    return material != null ? material : Material.BARRIER;
  }

  /** Check if this is a root node (no prerequisites). */
  @Contract(pure = true)
  public boolean isRoot() {
    return prerequisites.isEmpty();
  }

  /** Create a deep copy of this node. */
  @Contract(pure = true)
  public @NotNull EditorNode copy() {
    EditorNode copy = new EditorNode();
    copy.id = this.id;
    copy.name = this.name;
    copy.description = this.description;
    copy.icon = this.icon;
    copy.unlockedIcon = this.unlockedIcon;
    copy.itemModel = this.itemModel;
    copy.unlockedItemModel = this.unlockedItemModel;
    copy.cost = this.cost;
    copy.position = this.position; // Position is immutable
    copy.archetypeRef = this.archetypeRef;
    copy.perkId = this.perkId;
    copy.level = this.level;
    copy.prerequisites.addAll(this.prerequisites);
    copy.exclusive.addAll(this.exclusive);
    copy.children.addAll(this.children);
    copy.effects.addAll(this.effects.stream().map(EditorEffect::copy).toList());
    return copy;
  }
}
