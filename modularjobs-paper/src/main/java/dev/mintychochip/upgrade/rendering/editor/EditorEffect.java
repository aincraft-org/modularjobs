package dev.mintychochip.upgrade.rendering.editor;

import dev.mintychochip.upgrade.UpgradeEffect;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable effect data for editing purposes. Supports all effect types: boost, passive, permission,
 * ruled_boost.
 */
public final class EditorEffect {

  /** Effect type. */
  public enum EffectType {
    BOOST,
    PASSIVE,
    PERMISSION,
    RULED_BOOST
  }

  private EffectType type;

  // Boost fields
  private String target; // "xp", "money"
  private double amount; // multiplier (e.g., 1.1 for 10%)

  // Passive fields
  private String ability; // ability identifier
  private String passiveDescription;

  // Permission fields
  private String permission;
  private List<String> permissions;

  // Ruled boost fields (simplified for editor)
  private String ruledDescription;
  // Full ruled boost config stored as JSON string for now
  private String ruledConfigJson;

  /** Editor effect. */
  public EditorEffect() {
    this.type = EffectType.BOOST;
    this.target = "xp";
    this.amount = 1.1;
    this.permissions = new ArrayList<>();
  }

  /** Create from an existing UpgradeEffect. */
  @Contract(pure = true)
  public static @NotNull EditorEffect fromUpgradeEffect(@NotNull UpgradeEffect source) {
    EditorEffect effect = new EditorEffect();

    switch (source) {
      case UpgradeEffect.BoostEffect boost -> {
        effect.type = EffectType.BOOST;
        effect.target = boost.target();
        effect.amount = boost.multiplier().doubleValue();
      }
      case UpgradeEffect.PermissionEffect perm -> {
        effect.type = EffectType.PERMISSION;
        effect.permissions = new ArrayList<>(perm.permissions());
        effect.permission = perm.permission(); // For backward compatibility
      }
      case UpgradeEffect.RuledBoostEffect ruled -> {
        effect.type = EffectType.RULED_BOOST;
        effect.target = ruled.target();
        effect.ruledDescription = ruled.boostSource().description();
        // Store as simplified representation for now
        effect.ruledConfigJson = "{}"; // TODO: Serialize full config
      }
    }

    return effect;
  }

  // ========== Getters/Setters ==========

  /** Type. */
  @Contract(pure = true)
  public @Nullable EffectType type() {
    return type;
  }

  public void setType(@Nullable EffectType type) {
    this.type = type;
  }

  /** Target. */
  @Contract(pure = true)
  public @Nullable String target() {
    return target;
  }

  /** Sets the target. */
  public void setTarget(@Nullable String target) {
    this.target = target;
  }

  /** Amount. */
  @Contract(pure = true)
  public double amount() {
    return amount;
  }

  /** API member. */
  public void setAmount(double amount) {
    this.amount = amount;
  }

  /** Ability. */
  @Contract(pure = true)
  public @Nullable String ability() {
    return ability;
  }

  public void setAbility(@Nullable String ability) {
    this.ability = ability;
  }

  /** Passive description. */
  @Contract(pure = true)
  public @Nullable String passiveDescription() {
    return passiveDescription;
  }

  public void setPassiveDescription(@Nullable String desc) {
    this.passiveDescription = desc;
  }

  /** Permission. */
  @Contract(pure = true)
  public @Nullable String permission() {
    return permission;
  }

  public void setPermission(@Nullable String permission) {
    this.permission = permission;
  }

  /** Permissions. */
  @Contract(pure = true)
  public @NotNull List<String> permissions() {
    return permissions;
  }

  public void setPermissions(@Nullable List<String> permissions) {
    this.permissions = permissions != null ? permissions : new ArrayList<>();
  }

  /** Ruled description. */
  @Contract(pure = true)
  public @Nullable String ruledDescription() {
    return ruledDescription;
  }

  public void setRuledDescription(@Nullable String desc) {
    this.ruledDescription = desc;
  }

  /** Ruled config json. */
  @Contract(pure = true)
  public @Nullable String ruledConfigJson() {
    return ruledConfigJson;
  }

  public void setRuledConfigJson(@Nullable String json) {
    this.ruledConfigJson = json;
  }

  // ========== Display Helpers ==========

  /** Get a human-readable description of this effect. */
  @Contract(pure = true)
  public @NotNull String getDisplayDescription() {
    return switch (type) {
      case BOOST -> String.format("+%.0f%% %s", (amount - 1) * 100, target);
      case PASSIVE -> ability + (passiveDescription != null ? ": " + passiveDescription : "");
      case PERMISSION -> {
        if (permissions != null && !permissions.isEmpty()) {
          if (permissions.size() == 1) {
            yield "Permission: " + permissions.get(0);
          } else {
            yield "Permissions: " + String.join(", ", permissions);
          }
        } else {
          yield "Permission: " + permission;
        }
      }
      case RULED_BOOST -> ruledDescription != null ? ruledDescription : "Conditional boost";
    };
  }

  /** Create a deep copy of this effect. */
  @Contract(pure = true)
  public @NotNull EditorEffect copy() {
    EditorEffect copy = new EditorEffect();
    copy.type = this.type;
    copy.target = this.target;
    copy.amount = this.amount;
    copy.ability = this.ability;
    copy.passiveDescription = this.passiveDescription;
    copy.permission = this.permission;
    copy.permissions = new ArrayList<>(this.permissions);
    copy.ruledDescription = this.ruledDescription;
    copy.ruledConfigJson = this.ruledConfigJson;
    return copy;
  }
}
