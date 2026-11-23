package dev.mintychochip.databag.condition;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/** Immutable state belonging to a living entity, excluding player-only and location data. */
public record EntitySnapshot(
    Key entityType,
    boolean onFire,
    boolean onGround,
    boolean sneaking,
    boolean sprinting,
    boolean swimming,
    boolean baby,
    boolean gliding,
    @Nullable Double health,
    Map<Key, PotionEffectSnapshot> effects,
    Map<EquipmentSlotKey, ItemSubject> equipment,
    Set<Key> armorSet) implements SubjectSnapshot {

  public EntitySnapshot {
    Objects.requireNonNull(entityType, "entityType");
    effects = Map.copyOf(effects == null ? Map.of() : effects);
    equipment = Map.copyOf(equipment == null ? Map.of() : equipment);
    armorSet = Set.copyOf(armorSet == null ? Set.of() : armorSet);
  }

  public static Builder builder(Key entityType) {
    return new Builder(entityType);
  }

  public static final class Builder {
    private final Key entityType;
    private boolean onFire;
    private boolean onGround;
    private boolean sneaking;
    private boolean sprinting;
    private boolean swimming;
    private boolean baby;
    private boolean gliding;
    private @Nullable Double health;
    private Map<Key, PotionEffectSnapshot> effects = Map.of();
    private Map<EquipmentSlotKey, ItemSubject> equipment = Map.of();
    private Set<Key> armorSet = Set.of();

    private Builder(Key entityType) {
      this.entityType = Objects.requireNonNull(entityType, "entityType");
    }

    public Builder onFire(boolean value) {
      onFire = value;
      return this;
    }

    public Builder onGround(boolean value) {
      onGround = value;
      return this;
    }

    public Builder sneaking(boolean value) {
      sneaking = value;
      return this;
    }

    public Builder sprinting(boolean value) {
      sprinting = value;
      return this;
    }

    public Builder swimming(boolean value) {
      swimming = value;
      return this;
    }

    public Builder baby(boolean value) {
      baby = value;
      return this;
    }

    public Builder gliding(boolean value) {
      gliding = value;
      return this;
    }

    public Builder health(@Nullable Double value) {
      health = value;
      return this;
    }

    public Builder effects(Map<Key, PotionEffectSnapshot> value) {
      effects = value;
      return this;
    }

    public Builder equipment(Map<EquipmentSlotKey, ItemSubject> value) {
      equipment = value;
      return this;
    }

    public Builder armorSet(Set<Key> value) {
      armorSet = value;
      return this;
    }

    public EntitySnapshot build() {
      return new EntitySnapshot(
          entityType,
          onFire,
          onGround,
          sneaking,
          sprinting,
          swimming,
          baby,
          gliding,
          health,
          effects,
          equipment,
          armorSet);
    }
  }
}
