package dev.mintychochip.databag.condition;

import dev.mintychochip.databag.condition.builtin.KilledByPlayerCondition;
import dev.mintychochip.databag.DataBag;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable evaluation snapshot. May describe a player, a non-player living
 * entity, a block, or a combination (for example a player standing on a block).
 * Conditions fail closed when the fields they need are absent.
 *
 * <p>{@code present} means a <em>player</em> is in the snapshot. Living-entity
 * conditions use {@code livingPresent}; block conditions use {@code blockId}.
 *
 * @param present {@code true} when a player is in the snapshot
 * @param livingPresent {@code true} when a living entity is in the snapshot
 * @param death {@code true} when this snapshot is a death capture
 * @param sneaking {@code true} when the entity is sneaking
 * @param sprinting {@code true} when the entity is sprinting
 * @param onFire {@code true} when the entity is on fire
 * @param onGround {@code true} when the entity is on the ground
 * @param swimming {@code true} when the entity is swimming
 * @param baby {@code true} when the entity is a baby
 * @param flying {@code true} when the entity is flying
 * @param gliding {@code true} when the entity is gliding
 * @param entityType The entity type, or {@code null} when absent
 * @param gameMode The game mode, or {@code null} when absent
 * @param biome The biome, or {@code null} when absent
 * @param worldName The world name, or {@code null} when absent
 * @param worldKey The world key, or {@code null} when absent
 * @param weather The weather state, or {@code null} when absent
 * @param fluid The fluid at the entity, or {@code null} when absent
 * @param health The entity health, or {@code null} when absent
 * @param hunger The entity hunger, or {@code null} when absent
 * @param experience The experience amount, or {@code null} when absent
 * @param effects The active potion effects
 * @param jobKeys The entity's job keys
 * @param blockId The block identifier, or {@code null} when absent
 * @param blockProperties The block properties
 * @param dayTime The world day time, or {@code null} when absent
 * @param xpLevel The experience level, or {@code null} when absent
 * @param absorption The absorption amount, or {@code null} when absent
 * @param airRemaining The remaining air, or {@code null} when absent
 * @param attacker The attacking entity, or {@code null} when absent
 * @param directAttacker The direct attacking entity, or {@code null} when absent
 * @param attackingPlayer The attacking player, or {@code null} when absent
 * @param targetEntity The target entity, or {@code null} when absent
 * @param interactingEntity The interacting entity, or {@code null} when absent
 * @param x The x-coordinate, or {@code null} when absent
 * @param y The y-coordinate, or {@code null} when absent
 * @param z The z-coordinate, or {@code null} when absent
 * @param lightLevel The light level, or {@code null} when absent
 * @param skyLight The sky light level, or {@code null} when absent
 * @param blockLight The block light level, or {@code null} when absent
 * @param canSeeSky {@code true} when the sky is visible, or {@code null} when absent
 * @param team The team name, or {@code null} when absent
 * @param ping The player ping, or {@code null} when absent
 * @param emptyMain Empty slots in the main inventory, or {@code null} when absent
 * @param emptyHotbar Empty slots in the hotbar, or {@code null} when absent
 * @param emptyOffhand Empty offhand slots, or {@code null} when absent
 * @param sheared {@code true} when sheared, or {@code null} when absent
 * @param woolColor The wool color, or {@code null} when absent
 * @param collarColor The wolf collar color, or {@code null} when absent
 * @param frogVariant The frog variant, or {@code null} when absent
 * @param catVariant The cat variant, or {@code null} when absent
 * @param wolfVariant The wolf variant, or {@code null} when absent
 * @param tropicalFishVariant The tropical fish variant, or {@code null} when absent
 * @param tropicalFishPattern The tropical fish pattern, or {@code null} when absent
 * @param tropicalFishBaseColor The tropical fish base color, or {@code null} when absent
 * @param tropicalFishPatternColor The tropical fish pattern color, or {@code null} when absent
 * @param villagerType The villager type, or {@code null} when absent
 * @param villagerProfession The villager profession, or {@code null} when absent
 * @param villagerLevel The villager level, or {@code null} when absent
 * @param horseColor The horse color, or {@code null} when absent
 * @param horseArmor The horse armor, or {@code null} when absent
 * @param itemCounts Counts of each item
 * @param playtime The playtime, or {@code null} when absent
 * @param armorSet The equipped armor set
 * @param offhandItem The offhand item, or {@code null} when absent
 * @param itemSubject The subject item, or {@code null} when absent
 * @param equipment The equipped items by slot
 * @param activeCooldowns The active cooldowns
 * @param invulnerableTicks Remaining invulnerability ticks, or {@code null} when absent
 * @param advancements The completed advancements
 * @param vehicle The vehicle, or {@code null} when absent
 * @param passengers The passenger entities
 * @param ticksLived The lived ticks, or {@code null} when absent
 * @param random The random source, or {@code null} when absent
 * @param standingOn The block context beneath the entity, or {@code null} when absent
 * @param offsetResolver The offset resolver, or {@code null} when absent
 * @param scores The scoreboard scores
 * @param extras Additional values
 */
public record ConditionContext(
    boolean present,
    boolean livingPresent,
    boolean death,
    boolean sneaking,
    boolean sprinting,
    boolean onFire,
    boolean onGround,
    boolean swimming,
    boolean baby,
    boolean flying,
    boolean gliding,
    @Nullable Key entityType,
    @Nullable String gameMode,
    @Nullable Key biome,
    @Nullable String worldName,
    @Nullable Key worldKey,
    @Nullable WeatherState weather,
    @Nullable Key fluid,
    @Nullable Double health,
    @Nullable Double hunger,
    @Nullable Double experience,
    Map<Key, PotionEffectSnapshot> effects,
    Set<String> jobKeys,
    @Nullable Key blockId,
    Map<String, String> blockProperties,
    @Nullable Long dayTime,
    @Nullable Double xpLevel,
    @Nullable Double absorption,
    @Nullable Double airRemaining,
    @Nullable ConditionContext attacker,
    @Nullable ConditionContext directAttacker,
    @Nullable ConditionContext attackingPlayer,
    @Nullable ConditionContext targetEntity,
    @Nullable ConditionContext interactingEntity,
    @Nullable Double x,
    @Nullable Double y,
    @Nullable Double z,
    @Nullable Integer lightLevel,
    @Nullable Integer skyLight,
    @Nullable Integer blockLight,
    @Nullable Boolean canSeeSky,
    @Nullable String team,
    @Nullable Integer ping,
    @Nullable Integer emptyMain,
    @Nullable Integer emptyHotbar,
    @Nullable Integer emptyOffhand,
    @Nullable Boolean sheared,
    @Nullable Key woolColor,
    @Nullable Key collarColor,
    @Nullable Key frogVariant,
    @Nullable Key catVariant,
    @Nullable Key wolfVariant,
    @Nullable Integer tropicalFishVariant,
    @Nullable Key tropicalFishPattern,
    @Nullable Key tropicalFishBaseColor,
    @Nullable Key tropicalFishPatternColor,
    @Nullable Key villagerType,
    @Nullable Key villagerProfession,
    @Nullable Integer villagerLevel,
    @Nullable Key horseColor,
    @Nullable ItemSubject horseArmor,
    Map<Key, Integer> itemCounts,
    @Nullable Long playtime,
    Set<Key> armorSet,
    @Nullable ItemSnapshot offhandItem,
    @Nullable ItemSubject itemSubject,
    Map<EquipmentSlotKey, ItemSubject> equipment,
    Set<CooldownSnapshot> activeCooldowns,
    @Nullable Integer invulnerableTicks,
    Set<String> advancements,
    @Nullable ConditionContext vehicle,
    List<ConditionContext> passengers,
    @Nullable Integer ticksLived,
    @Nullable Random random,
    @Nullable ConditionContext standingOn,
    @Nullable OffsetContextResolver offsetResolver,
    Map<String, Integer> scores,
    DataBag extras
) {

  /** Normalizes null collections to empty immutable copies and defensively copies the extras bag. */
  public ConditionContext {
    effects = Map.copyOf(effects == null ? Map.of() : effects);
    jobKeys = Set.copyOf(jobKeys == null ? Set.of() : jobKeys);
    blockProperties = Map.copyOf(blockProperties == null ? Map.of() : blockProperties);
    itemCounts = Map.copyOf(itemCounts == null ? Map.of() : itemCounts);
    armorSet = Set.copyOf(armorSet == null ? Set.of() : armorSet);
    offhandItem = offhandItem;
    itemSubject = itemSubject;
    equipment = Map.copyOf(equipment == null ? Map.of() : equipment);
    activeCooldowns = Set.copyOf(activeCooldowns == null ? Set.of() : activeCooldowns);
    invulnerableTicks = invulnerableTicks;
    advancements = Set.copyOf(advancements == null ? Set.of() : advancements);
    vehicle = vehicle;
    passengers = List.copyOf(passengers == null ? List.of() : passengers);
    scores = Map.copyOf(scores == null ? Map.of() : scores);
    standingOn = standingOn;
    offsetResolver = offsetResolver;
    extras = extras == null ? DataBag.create() : DataBag.fromBytes(extras.toBytes());
  }

  /**
   * Extension snapshot data for registered {@link ConditionHandler}s.
   *
   * @return a decoded copy of the extension bag
   */
  public DataBag extras() {
    return DataBag.fromBytes(extras.toBytes());
  }

  /**
   * Returns a snapshot with no player or living entity present.
   *
   * @return a context with no player or living entity present
   */
  public static ConditionContext absent() {
    return builder().present(false).livingPresent(false).build();
  }

  /**
   * Returns a new builder with player and living entity marked present.
   *
   * @return a new builder with player and living entity marked present
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder for {@link ConditionContext}. */
  public static final class Builder {

    private boolean present = true;
    private boolean livingPresent = true;
    private boolean death;
    private boolean sneaking;
    private boolean sprinting;
    private boolean onFire;
    private boolean onGround;
    private boolean swimming;
    private boolean baby;
    private boolean flying;
    private boolean gliding;
    private @Nullable Key entityType;
    private @Nullable String gameMode;
    private @Nullable Key biome;
    private @Nullable String worldName;
    private @Nullable Key worldKey;
    private @Nullable WeatherState weather;
    private @Nullable Key fluid;
    private @Nullable Double health;
    private @Nullable Double hunger;
    private @Nullable Double experience;
    private Map<Key, PotionEffectSnapshot> effects = Map.of();
    private Set<String> jobKeys = Set.of();
    private @Nullable Key blockId;
    private Map<String, String> blockProperties = Map.of();
    private @Nullable Long dayTime;
    private @Nullable Double xpLevel;
    private @Nullable Double absorption;
    private @Nullable Double airRemaining;
    private @Nullable ConditionContext attacker;
    private @Nullable ConditionContext directAttacker;
    private @Nullable ConditionContext attackingPlayer;
    private @Nullable ConditionContext targetEntity;
    private @Nullable ConditionContext interactingEntity;
    private @Nullable Double x;
    private @Nullable Double y;
    private @Nullable Double z;
    private @Nullable Integer lightLevel;
    private @Nullable Integer skyLight;
    private @Nullable Integer blockLight;
    private @Nullable Boolean canSeeSky;
    private @Nullable String team;
    private @Nullable Integer ping;
    private @Nullable Integer emptyMain;
    private @Nullable Integer emptyHotbar;
    private @Nullable Integer emptyOffhand;
    private @Nullable Boolean sheared;
    private @Nullable Key woolColor;
    private @Nullable Key collarColor;
    private @Nullable Key frogVariant;
    private @Nullable Key catVariant;
    private @Nullable Key wolfVariant;
    private @Nullable Integer tropicalFishVariant;
    private @Nullable Key tropicalFishPattern;
    private @Nullable Key tropicalFishBaseColor;
    private @Nullable Key tropicalFishPatternColor;
    private @Nullable Key villagerType;
    private @Nullable Key villagerProfession;
    private @Nullable Integer villagerLevel;
    private @Nullable Key horseColor;
    private @Nullable ItemSubject horseArmor;
    private Map<Key, Integer> itemCounts = Map.of();
    private @Nullable Long playtime;
    private Set<Key> armorSet = Set.of();
    private @Nullable ItemSnapshot offhandItem;
    private @Nullable ItemSubject itemSubject;
    private Map<EquipmentSlotKey, ItemSubject> equipment = Map.of();
    private Set<CooldownSnapshot> activeCooldowns = Set.of();
    private @Nullable Integer invulnerableTicks;
    private Set<String> advancements = Set.of();
    private @Nullable ConditionContext vehicle;
    private List<ConditionContext> passengers = List.of();
    private @Nullable Integer ticksLived;
    private @Nullable Random random;
    private @Nullable ConditionContext standingOn;
    private @Nullable OffsetContextResolver offsetResolver;
    private Map<String, Integer> scores = Map.of();
    private DataBag extras = DataBag.create();
    private boolean livingPresentOverridden;

    private Builder() {}

    /**
     * Marks a player as present. Also marks a living entity present unless
     * {@link #livingPresent(boolean)} was set explicitly.
     *
     * @param present whether a player is present
     *
     * @return this builder
     */
    public Builder present(boolean present) {
      this.present = present;
      if (!livingPresentOverridden && present) {
        this.livingPresent = true;
      }
      if (!present && !livingPresentOverridden) {
        this.livingPresent = false;
      }
      return this;
    }

    /**
     * Sets living-entity presence explicitly, overriding {@link #present(boolean)} side effects.
     *
     * @param livingPresent whether a living entity is present
     *
     * @return this builder
     */
    public Builder livingPresent(boolean livingPresent) {
      this.livingPresent = livingPresent;
      this.livingPresentOverridden = true;
      return this;
    }

    /**
     * Marks this snapshot as a death capture for {@link KilledByPlayerCondition}.
     *
     * @param death whether this context is a death snapshot
     * @return this builder
     */
    public Builder death(boolean death) {
      this.death = death;
      return this;
    }

    /**
     * Sets whether the entity is sneaking.
     *
     * @param sneaking whether the entity is sneaking
     *
     * @return this builder
     */
    public Builder sneaking(boolean sneaking) {
      this.sneaking = sneaking;
      return this;
    }

    /**
     * Sets whether the entity is sprinting.
     *
     * @param sprinting whether the entity is sprinting
     *
     * @return this builder
     */
    public Builder sprinting(boolean sprinting) {
      this.sprinting = sprinting;
      return this;
    }

    /**
     * Sets whether the entity is on fire.
     *
     * @param onFire whether the entity is on fire
     *
     * @return this builder
     */
    public Builder onFire(boolean onFire) {
      this.onFire = onFire;
      return this;
    }

    /**
     * Sets whether the entity is on the ground.
     *
     * @param onGround whether the entity is on the ground
     *
     * @return this builder
     */
    public Builder onGround(boolean onGround) {
      this.onGround = onGround;
      return this;
    }

    /**
     * Sets whether the entity is swimming.
     *
     * @param swimming whether the entity is swimming
     *
     * @return this builder
     */
    public Builder swimming(boolean swimming) {
      this.swimming = swimming;
      return this;
    }

    /**
     * Sets whether the entity is a baby.
     *
     * @param baby whether the entity is a baby
     *
     * @return this builder
     */
    public Builder baby(boolean baby) {
      this.baby = baby;
      return this;
    }

    /**
     * Sets whether the entity is flying.
     *
     * @param flying whether the entity is flying
     *
     * @return this builder
     */
    public Builder flying(boolean flying) {
      this.flying = flying;
      return this;
    }

    /**
     * Sets whether the entity is gliding.
     *
     * @param gliding whether the entity is gliding
     *
     * @return this builder
     */
    public Builder gliding(boolean gliding) {
      this.gliding = gliding;
      return this;
    }

    /**
     * Sets the entity type, or {@code null} to clear it.
     *
     * @param entityType the entityType, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder entityType(@Nullable Key entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * Sets the game mode, or {@code null} to clear it.
     *
     * @param gameMode the gameMode, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder gameMode(@Nullable String gameMode) {
      this.gameMode = gameMode;
      return this;
    }

    /**
     * Sets the biome, or {@code null} to clear it.
     *
     * @param biome the biome, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder biome(@Nullable Key biome) {
      this.biome = biome;
      return this;
    }

    /**
     * Sets the world name, or {@code null} to clear it.
     *
     * @param worldName the worldName, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder worldName(@Nullable String worldName) {
      this.worldName = worldName;
      return this;
    }

    /**
     * Sets the world key, or {@code null} to clear it.
     *
     * @param worldKey the worldKey, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder worldKey(@Nullable Key worldKey) {
      this.worldKey = worldKey;
      return this;
    }

    /**
     * Sets the weather state, or {@code null} to clear it.
     *
     * @param weather the weather, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder weather(@Nullable WeatherState weather) {
      this.weather = weather;
      return this;
    }

    /**
     * Sets the fluid at the entity, or {@code null} to clear it.
     *
     * @param fluid the fluid, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder fluid(@Nullable Key fluid) {
      this.fluid = fluid;
      return this;
    }

    /**
     * Sets the entity health, or {@code null} to clear it.
     *
     * @param health the health, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder health(@Nullable Double health) {
      this.health = health;
      return this;
    }

    /**
     * Sets the player hunger, or {@code null} to clear it.
     *
     * @param hunger the hunger, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder hunger(@Nullable Double hunger) {
      this.hunger = hunger;
      return this;
    }

    /**
     * Sets the world day time, or {@code null} to clear it.
     *
     * @param dayTime the dayTime, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder dayTime(@Nullable Long dayTime) {
      this.dayTime = dayTime;
      return this;
    }

    /**
     * Sets the player's experience level, or {@code null} to clear it.
     *
     * @param xpLevel the xpLevel, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder xpLevel(@Nullable Double xpLevel) {
      this.xpLevel = xpLevel;
      return this;
    }

    /**
     * Sets the absorption amount, or {@code null} to clear it.
     *
     * @param absorption the absorption, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder absorption(@Nullable Double absorption) {
      this.absorption = absorption;
      return this;
    }

    /**
     * Sets the remaining air, or {@code null} to clear it.
     *
     * @param airRemaining the airRemaining, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder airRemaining(@Nullable Double airRemaining) {
      this.airRemaining = airRemaining;
      return this;
    }

    /**
     * Sets the attacker snapshot, or {@code null} to clear it.
     *
     * @param attacker the attacker, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder attacker(@Nullable ConditionContext attacker) {
      this.attacker = attacker;
      return this;
    }

    /**
     * Sets the direct attacker snapshot, or {@code null} to clear it.
     *
     * @param directAttacker the directAttacker, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder directAttacker(@Nullable ConditionContext directAttacker) {
      this.directAttacker = directAttacker;
      return this;
    }

    /**
     * Sets the attacking player snapshot, or {@code null} to clear it.
     *
     * @param attackingPlayer the attackingPlayer, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder attackingPlayer(@Nullable ConditionContext attackingPlayer) {
      this.attackingPlayer = attackingPlayer;
      return this;
    }

    /**
     * Sets the target entity snapshot, or {@code null} to clear it.
     *
     * @param targetEntity the targetEntity, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder targetEntity(@Nullable ConditionContext targetEntity) {
      this.targetEntity = targetEntity;
      return this;
    }

    /**
     * Sets the interacting entity snapshot, or {@code null} to clear it.
     *
     * @param interactingEntity the interactingEntity, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder interactingEntity(@Nullable ConditionContext interactingEntity) {
      this.interactingEntity = interactingEntity;
      return this;
    }

    /**
     * Sets the x-coordinate, or {@code null} to clear it.
     *
     * @param x the x, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder x(@Nullable Double x) {
      this.x = x;
      return this;
    }

    /**
     * Sets the y-coordinate, or {@code null} to clear it.
     *
     * @param y the y, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder y(@Nullable Double y) {
      this.y = y;
      return this;
    }

    /**
     * Sets the z-coordinate, or {@code null} to clear it.
     *
     * @param z the z, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder z(@Nullable Double z) {
      this.z = z;
      return this;
    }

    /**
     * Sets the combined light level, or {@code null} to clear it.
     *
     * @param lightLevel the lightLevel, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder lightLevel(@Nullable Integer lightLevel) {
      this.lightLevel = lightLevel;
      return this;
    }

    /**
     * Sets the sky light level, or {@code null} to clear it.
     *
     * @param skyLight the skyLight, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder skyLight(@Nullable Integer skyLight) {
      this.skyLight = skyLight;
      return this;
    }

    /**
     * Sets the block light level, or {@code null} to clear it.
     *
     * @param blockLight the blockLight, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder blockLight(@Nullable Integer blockLight) {
      this.blockLight = blockLight;
      return this;
    }

    /**
     * Sets whether the location can see the sky, or {@code null} to clear it.
     *
     * @param canSeeSky the canSeeSky, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder canSeeSky(@Nullable Boolean canSeeSky) {
      this.canSeeSky = canSeeSky;
      return this;
    }

    /**
     * Sets the entity team, or {@code null} to clear it.
     *
     * @param team the team, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder team(@Nullable String team) {
      this.team = team;
      return this;
    }

    /**
     * Sets the player latency, or {@code null} to clear it.
     *
     * @param ping the ping, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder ping(@Nullable Integer ping) {
      this.ping = ping;
      return this;
    }

    /**
     * Sets the number of empty main-inventory slots, or {@code null} to clear it.
     *
     * @param emptyMain the emptyMain, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder emptyMain(@Nullable Integer emptyMain) {
      this.emptyMain = emptyMain;
      return this;
    }

    /**
     * Sets the number of empty hotbar slots, or {@code null} to clear it.
     *
     * @param emptyHotbar the emptyHotbar, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder emptyHotbar(@Nullable Integer emptyHotbar) {
      this.emptyHotbar = emptyHotbar;
      return this;
    }

    /**
     * Sets the number of empty offhand slots, or {@code null} to clear it.
     *
     * @param emptyOffhand the emptyOffhand, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder emptyOffhand(@Nullable Integer emptyOffhand) {
      this.emptyOffhand = emptyOffhand;
      return this;
    }

    /**
     * Sets whether the entity is sheared, or {@code null} to clear it.
     *
     * @param sheared the sheared, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder sheared(@Nullable Boolean sheared) {
      this.sheared = sheared;
      return this;
    }

    /**
     * Sets the wool color, or {@code null} to clear it.
     *
     * @param woolColor the woolColor, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder woolColor(@Nullable Key woolColor) {
      this.woolColor = woolColor;
      return this;
    }

    /**
     * Sets the wolf collar color, or {@code null} to clear it.
     *
     * @param collarColor the collar color, or {@code null} to clear it
     * @return this builder
     */
    public Builder collarColor(@Nullable Key collarColor) {
      this.collarColor = collarColor;
      return this;
    }

    /**
     * Sets the frog variant, or {@code null} to clear it.
     *
     * @param frogVariant the frogVariant, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder frogVariant(@Nullable Key frogVariant) {
      this.frogVariant = frogVariant;
      return this;
    }

    /**
     * Sets the cat variant, or {@code null} to clear it.
     *
     * @param catVariant the catVariant, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder catVariant(@Nullable Key catVariant) {
      this.catVariant = catVariant;
      return this;
    }
    /**
     * Sets the wolf variant, or {@code null} to clear it.
     *
     * @param wolfVariant the wolfVariant, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder wolfVariant(@Nullable Key wolfVariant) {
      this.wolfVariant = wolfVariant;
      return this;
    }
    /**
     * Sets the tropical fish variant, or {@code null} to clear it.
     *
     * @param tropicalFishVariant the tropicalFishVariant, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder tropicalFishVariant(@Nullable Integer tropicalFishVariant) {
      this.tropicalFishVariant = tropicalFishVariant;
      return this;
    }
    /**
     * Sets the tropical fish pattern, or {@code null} to clear it.
     *
     * @param tropicalFishPattern the tropicalFishPattern, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder tropicalFishPattern(@Nullable Key tropicalFishPattern) {
      this.tropicalFishPattern = tropicalFishPattern;
      return this;
    }
    /**
     * Sets the tropical fish base color, or {@code null} to clear it.
     *
     * @param tropicalFishBaseColor the tropicalFishBaseColor, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder tropicalFishBaseColor(@Nullable Key tropicalFishBaseColor) {
      this.tropicalFishBaseColor = tropicalFishBaseColor;
      return this;
    }
    /**
     * Sets the tropical fish pattern color, or {@code null} to clear it.
     *
     * @param tropicalFishPatternColor the tropicalFishPatternColor, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder tropicalFishPatternColor(@Nullable Key tropicalFishPatternColor) {
      this.tropicalFishPatternColor = tropicalFishPatternColor;
      return this;
    }
    /**
     * Sets the villager type, or {@code null} to clear it.
     *
     * @param villagerType the villagerType, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder villagerType(@Nullable Key villagerType) {
      this.villagerType = villagerType;
      return this;
    }
    /**
     * Sets the villager profession, or {@code null} to clear it.
     *
     * @param villagerProfession the villagerProfession, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder villagerProfession(@Nullable Key villagerProfession) {
      this.villagerProfession = villagerProfession;
      return this;
    }
    /**
     * Sets the villager level, or {@code null} to clear it.
     *
     * @param villagerLevel the villagerLevel, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder villagerLevel(@Nullable Integer villagerLevel) {
      this.villagerLevel = villagerLevel;
      return this;
    }
    /**
     * Sets the horse color, or {@code null} to clear it.
     *
     * @param horseColor the horseColor, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder horseColor(@Nullable Key horseColor) {
      this.horseColor = horseColor;
      return this;
    }
    /**
     * Sets the horse armor, or {@code null} to clear it.
     *
     * @param horseArmor the horseArmor, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder horseArmor(@Nullable ItemSubject horseArmor) {
      this.horseArmor = horseArmor;
      return this;
    }

    /**
     * Replaces the item counts map.
     *
     * @param itemCounts item counts by material
     *
     * @return this builder
     */
    public Builder itemCounts(Map<Key, Integer> itemCounts) {
      this.itemCounts = Objects.requireNonNull(itemCounts);
      return this;
    }

    /**
     * Sets the player's playtime, or {@code null} to clear it.
     *
     * @param playtime the playtime, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder playtime(@Nullable Long playtime) {
      this.playtime = playtime;
      return this;
    }

    /**
     * Replaces the armor item types.
     *
     * @param armorSet the armorSet
     *
     * @return this builder
     */
    public Builder armorSet(Set<Key> armorSet) {
      this.armorSet = Objects.requireNonNull(armorSet);
      return this;
    }
    /**
     * Sets the offhand item snapshot, or {@code null} to clear it.
     *
     * @param offhandItem the offhandItem, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder offhandItem(@Nullable ItemSnapshot offhandItem) {
      this.offhandItem = offhandItem;
      return this;
    }
    /**
     * Sets the subject item, or {@code null} to clear it.
     *
     * @param itemSubject the itemSubject, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder itemSubject(@Nullable ItemSubject itemSubject) {
      this.itemSubject = itemSubject;
      return this;
    }
    /**
     * Replaces the equipped items by slot.
     *
     * @param equipment equipped items by slot
     *
     * @return this builder
     */
    public Builder equipment(Map<EquipmentSlotKey, ItemSubject> equipment) {
      this.equipment = new java.util.HashMap<>(Objects.requireNonNull(equipment));
      return this;
    }
    /**
     * Sets one equipment slot; {@code null} subject becomes {@link ItemSubject#empty()}.
     *
     * @param slot the equipment slot
     * @param subject the item subject, or {@code null} to use {@link ItemSubject#empty()}
     *
     * @return this builder
     */
    public Builder equipmentSlot(EquipmentSlotKey slot, @Nullable ItemSubject subject) {
      if (this.equipment == null || this.equipment.isEmpty()) {
        this.equipment = new java.util.HashMap<>();
      }
      this.equipment.put(Objects.requireNonNull(slot), subject == null ? ItemSubject.empty() : subject);
      return this;
    }
    /**
     * Replaces the active cooldowns.
     *
     * @param activeCooldowns the activeCooldowns
     *
     * @return this builder
     */
    public Builder activeCooldowns(Set<CooldownSnapshot> activeCooldowns) {
      this.activeCooldowns = new java.util.HashSet<>(Objects.requireNonNull(activeCooldowns));
      return this;
    }
    /**
     * Adds one active cooldown.
     *
     * @param snapshot the cooldown snapshot
     *
     * @return this builder
     */
    public Builder activeCooldown(CooldownSnapshot snapshot) {
      if (this.activeCooldowns == null || this.activeCooldowns.isEmpty()) {
        this.activeCooldowns = new java.util.HashSet<>();
      }
      this.activeCooldowns.add(Objects.requireNonNull(snapshot));
      return this;
    }

    /**
     * Sets the remaining invulnerability ticks, or {@code null} to clear it.
     *
     * @param invulnerableTicks the invulnerableTicks, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder invulnerableTicks(@Nullable Integer invulnerableTicks) {
      this.invulnerableTicks = invulnerableTicks;
      return this;
    }

    /**
     * Replaces the completed advancement keys.
     *
     * @param advancements the advancements
     *
     * @return this builder
     */
    public Builder advancements(Set<String> advancements) {
      this.advancements = Objects.requireNonNull(advancements);
      return this;
    }

    /**
     * Sets the vehicle snapshot, or {@code null} to clear it.
     *
     * @param vehicle the vehicle, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder vehicle(@Nullable ConditionContext vehicle) {
      this.vehicle = vehicle;
      return this;
    }

    /**
     * Replaces the passenger snapshots.
     *
     * @param passengers the passengers
     *
     * @return this builder
     */
    public Builder passengers(List<ConditionContext> passengers) {
      this.passengers = Objects.requireNonNull(passengers);
      return this;
    }

    /**
     * Sets the entity age in ticks, or {@code null} to clear it.
     *
     * @param ticksLived the ticksLived, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder ticksLived(@Nullable Integer ticksLived) {
      this.ticksLived = ticksLived;
      return this;
    }

    /**
     * Sets the random source, or {@code null} to clear it.
     *
     * @param random the random, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder random(@Nullable Random random) {
      this.random = random;
      return this;
    }
    /**
     * Sets the block or entity stood on, or {@code null} to clear it.
     *
     * @param standingOn the standingOn, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder standingOn(@Nullable ConditionContext standingOn) {
      this.standingOn = standingOn;
      return this;
    }
    /**
     * Sets the offset resolver, or {@code null} to clear it.
     *
     * @param offsetResolver the offsetResolver, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder offsetResolver(@Nullable OffsetContextResolver offsetResolver) {
      this.offsetResolver = offsetResolver;
      return this;
    }

    /**
     * Replaces the scoreboard values by objective.
     *
     * @param scores scoreboard values by objective
     *
     * @return this builder
     */
    public Builder scores(Map<String, Integer> scores) {
      this.scores = Objects.requireNonNull(scores);
      return this;
    }
    /**
     * Sets the player experience, or {@code null} to clear it.
     *
     * @param experience the experience, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder experience(@Nullable Double experience) {
      this.experience = experience;
      return this;
    }

    /**
     * Replaces the active potion effects.
     *
     * @param effects active potion effects
     *
     * @return this builder
     */
    public Builder effects(Map<Key, PotionEffectSnapshot> effects) {
      this.effects = Objects.requireNonNull(effects);
      return this;
    }

    /**
     * Replaces the player's job keys.
     *
     * @param jobKeys the jobKeys
     *
     * @return this builder
     */
    public Builder jobKeys(Set<String> jobKeys) {
      this.jobKeys = Objects.requireNonNull(jobKeys);
      return this;
    }

    /**
     * Sets the block type, or {@code null} to clear it.
     *
     * @param blockId the blockId, or {@code null} to clear it
     *
     * @return this builder
     */
    public Builder blockId(@Nullable Key blockId) {
      this.blockId = blockId;
      return this;
    }

    /**
     * Replaces the block properties.
     *
     * @param blockProperties block state properties
     *
     * @return this builder
     */
    public Builder blockProperties(Map<String, String> blockProperties) {
      this.blockProperties = Objects.requireNonNull(blockProperties);
      return this;
    }

    /**
     * Sets extension data; {@code null} becomes an empty bag.
     *
     * @param extras the extension data, or {@code null} for an empty bag
     *
     * @return this builder
     */
    public Builder extras(DataBag extras) {
      this.extras = extras == null ? DataBag.create() : extras;
      return this;
    }

    /**
     * Builds an immutable snapshot from the configured values.
     *
     * @return the built immutable context
     */
    public ConditionContext build() {
      return new ConditionContext(
          present,
          livingPresent,
          death,
          sneaking,
          sprinting,
          onFire,
          onGround,
          swimming,
          baby,
          flying,
          gliding,
          entityType,
          gameMode,
          biome,
          worldName,
          worldKey,
          weather,
          fluid,
          health,
          hunger,
          experience,
          effects,
          jobKeys,
          blockId,
          blockProperties,
          dayTime,
          xpLevel,
          absorption,
          airRemaining,
          attacker,
          directAttacker,
          attackingPlayer,
          targetEntity,
          interactingEntity,
          x,
          y,
          z,
          lightLevel,
          skyLight,
          blockLight,
          canSeeSky,
          team,
          ping,
          emptyMain,
          emptyHotbar,
          emptyOffhand,
          sheared,
          woolColor,
          collarColor,
          frogVariant,
          catVariant,
          wolfVariant,
          tropicalFishVariant,
          tropicalFishPattern,
          tropicalFishBaseColor,
          tropicalFishPatternColor,
          villagerType,
          villagerProfession,
          villagerLevel,
          horseColor,
          horseArmor,
          itemCounts,
          playtime,
          armorSet,
          offhandItem,
          itemSubject,
          equipment,
          activeCooldowns,
          invulnerableTicks,
          advancements,
          vehicle,
          passengers,
          ticksLived,
          random,
          standingOn,
          offsetResolver,
          scores,
          extras);
    }
  }
}
