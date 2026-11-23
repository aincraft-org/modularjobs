package dev.mintychochip.databag.condition;

import dev.mintychochip.databag.condition.builtin.ActiveCooldownCondition;
import dev.mintychochip.databag.condition.builtin.AdvancementCondition;
import dev.mintychochip.databag.condition.builtin.AllOfCondition;
import dev.mintychochip.databag.condition.builtin.AlwaysCondition;
import dev.mintychochip.databag.condition.builtin.AnyOfCondition;
import dev.mintychochip.databag.condition.builtin.ArmorSetCondition;
import dev.mintychochip.databag.condition.builtin.BabyCondition;
import dev.mintychochip.databag.condition.builtin.BiomeCondition;
import dev.mintychochip.databag.condition.builtin.BlockIdCondition;
import dev.mintychochip.databag.condition.builtin.BlockPropertyCondition;
import dev.mintychochip.databag.condition.builtin.BlockPropertyRangeCondition;
import dev.mintychochip.databag.condition.builtin.CanSeeSkyCondition;
import dev.mintychochip.databag.condition.builtin.CatCondition;
import dev.mintychochip.databag.condition.builtin.DimensionCondition;
import dev.mintychochip.databag.condition.builtin.EmptySlotsCondition;
import dev.mintychochip.databag.condition.builtin.EntityScoresCondition;
import dev.mintychochip.databag.condition.builtin.EntityTargetCondition;
import dev.mintychochip.databag.condition.builtin.EntityTypeCondition;
import dev.mintychochip.databag.condition.builtin.EquipmentItemCondition;
import dev.mintychochip.databag.condition.builtin.FluidCondition;
import dev.mintychochip.databag.condition.builtin.FlyingCondition;
import dev.mintychochip.databag.condition.builtin.FrogCondition;
import dev.mintychochip.databag.condition.builtin.GameModeCondition;
import dev.mintychochip.databag.condition.builtin.GlidingCondition;
import dev.mintychochip.databag.condition.builtin.HorseCondition;
import dev.mintychochip.databag.condition.builtin.InvertedCondition;
import dev.mintychochip.databag.condition.builtin.InvulnerableFramesCondition;
import dev.mintychochip.databag.condition.builtin.ItemDurabilityCondition;
import dev.mintychochip.databag.condition.builtin.ItemEnchantmentsCondition;
import dev.mintychochip.databag.condition.builtin.ItemTrimCondition;
import dev.mintychochip.databag.condition.builtin.ItemTypeCountCondition;
import dev.mintychochip.databag.condition.builtin.JobCondition;
import dev.mintychochip.databag.condition.builtin.KilledByPlayerCondition;
import dev.mintychochip.databag.condition.builtin.LightCondition;
import dev.mintychochip.databag.condition.builtin.LocationOffsetCondition;
import dev.mintychochip.databag.condition.builtin.MatchBlockCondition;
import dev.mintychochip.databag.condition.builtin.OffhandItemCondition;
import dev.mintychochip.databag.condition.builtin.OnFireCondition;
import dev.mintychochip.databag.condition.builtin.OnGroundCondition;
import dev.mintychochip.databag.condition.builtin.PassengerCondition;
import dev.mintychochip.databag.condition.builtin.PeriodicTickCondition;
import dev.mintychochip.databag.condition.builtin.PingCondition;
import dev.mintychochip.databag.condition.builtin.PlayerResourceCondition;
import dev.mintychochip.databag.condition.builtin.PlaytimeCondition;
import dev.mintychochip.databag.condition.builtin.PositionCondition;
import dev.mintychochip.databag.condition.builtin.PotionAmplifierCondition;
import dev.mintychochip.databag.condition.builtin.PotionDurationCondition;
import dev.mintychochip.databag.condition.builtin.PotionPresentCondition;
import dev.mintychochip.databag.condition.builtin.RandomChanceCondition;
import dev.mintychochip.databag.condition.builtin.RequiredItemCountCondition;
import dev.mintychochip.databag.condition.builtin.SheepCondition;
import dev.mintychochip.databag.condition.builtin.SneakingCondition;
import dev.mintychochip.databag.condition.builtin.SprintingCondition;
import dev.mintychochip.databag.condition.builtin.SteppingOnCondition;
import dev.mintychochip.databag.condition.builtin.SwimmingCondition;
import dev.mintychochip.databag.condition.builtin.TeamCondition;
import dev.mintychochip.databag.condition.builtin.TimeCheckCondition;
import dev.mintychochip.databag.condition.builtin.TropicalFishCondition;
import dev.mintychochip.databag.condition.builtin.VehicleCondition;
import dev.mintychochip.databag.condition.builtin.VillagerCondition;
import dev.mintychochip.databag.condition.builtin.WeatherCondition;
import dev.mintychochip.databag.condition.builtin.WolfVariantCondition;
import dev.mintychochip.databag.condition.builtin.WorldCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Factories and combinator operations for the built-in {@link Condition} graph.
 *
 * <p>The public construction surface stays in this API package; concrete predicate
 * implementations live in {@code dev.mintychochip.databag.condition.builtin}.
 * No Bridge / Bukkit.
 */
public final class Conditions {

  private Conditions() {}

  /**
   * Returns a condition that always evaluates to {@code true}.
   *
   * @return the built condition
   */
  public static Condition always() {
    return AlwaysCondition.INSTANCE;
  }

  /**
   * Returns a condition matching the entity's sneaking state to {@code expected}.
   *
   * @param expected the required sneaking state
   * @return the built condition
   */
  public static Condition sneaking(boolean expected) {
    return new SneakingCondition(expected);
  }

  /**
   * Returns a condition matching the entity's sprinting state to {@code expected}.
   *
   * @param expected the required sprinting state
   * @return the built condition
   */
  public static Condition sprinting(boolean expected) {
    return new SprintingCondition(expected);
  }

  /**
   * Returns a condition matching the entity type to {@code entityType}.
   *
   * @param entityType the required entity type
   * @return the built condition
   */
  public static Condition entityType(Key entityType) {
    return new EntityTypeCondition(Objects.requireNonNull(entityType));
  }

  /**
   * Returns a condition matching the entity's on-fire state to {@code expected}.
   *
   * @param expected the required on-fire state
   * @return the built condition
   */
  public static Condition onFire(boolean expected) {
    return new OnFireCondition(expected);
  }

  /**
   * Returns a condition matching the entity's on-ground state to {@code expected}.
   *
   * @param expected the required on-ground state
   * @return the built condition
   */
  public static Condition onGround(boolean expected) {
    return new OnGroundCondition(expected);
  }

  /**
   * Returns a condition matching the entity's swimming state to {@code expected}.
   *
   * @param expected the required swimming state
   * @return the built condition
   */
  public static Condition swimming(boolean expected) {
    return new SwimmingCondition(expected);
  }

  /**
   * Returns a condition matching the entity's baby state to {@code expected}.
   *
   * @param expected the required baby state
   * @return the built condition
   */
  public static Condition baby(boolean expected) {
    return new BabyCondition(expected);
  }

  /**
   * Returns a condition matching the entity's gliding state to {@code expected}.
   *
   * @param expected the required gliding state
   * @return the built condition
   */
  public static Condition gliding(boolean expected) {
    return new GlidingCondition(expected);
  }

  /**
   * Returns a condition matching the entity's flying state to {@code expected}.
   *
   * @param expected the required flying state
   * @return the built condition
   */
  public static Condition flying(boolean expected) {
    return new FlyingCondition(expected);
  }

  /**
   * Returns a condition matching the entity's game mode to {@code gameMode}.
   *
   * @param gameMode the required game mode
   * @return the built condition
   */
  public static Condition gameMode(String gameMode) {
    return new GameModeCondition(gameMode);
  }

  /**
   * Returns a condition matching the block identifier to {@code blockId}.
   *
   * @param blockId the required block identifier
   * @return the built condition
   */
  public static Condition blockId(Key blockId) {
    return new BlockIdCondition(Objects.requireNonNull(blockId));
  }

  /**
   * Returns a condition matching a block property name and value.
   *
   * @param name the block property name
   * @param value the required property value
   * @return the built condition
   */
  public static Condition blockProperty(String name, String value) {
    return new BlockPropertyCondition(name, value);
  }

  /**
   * Returns a condition matching the biome identifier to {@code biomeKey}.
   *
   * @param biomeKey the required biome identifier
   * @return the built condition
   */
  public static Condition biome(Key biomeKey) {
    return new BiomeCondition(Objects.requireNonNull(biomeKey));
  }

  /**
   * Returns a condition matching the world name to {@code worldName}.
   *
   * @param worldName the required world name
   * @return the built condition
   */
  public static Condition world(String worldName) {
    return new WorldCondition(worldName);
  }

  /**
   * Returns a condition matching the weather state to {@code state}.
   *
   * @param state the required weather state
   * @return the built condition
   */
  public static Condition weather(WeatherState state) {
    return new WeatherCondition(Objects.requireNonNull(state));
  }

  /**
   * Returns a condition matching the fluid identifier to {@code fluidKey}.
   *
   * @param fluidKey the required fluid identifier
   * @return the built condition
   */
  public static Condition fluid(Key fluidKey) {
    return new FluidCondition(Objects.requireNonNull(fluidKey));
  }

  /**
   * Returns a condition checking the time bounds and period.
   *
   * @param min the minimum time bound
   * @param max the maximum time bound
   * @param period the time period
   * @return the built condition
   */
  public static Condition timeCheck(Long min, Long max, Long period) {
    return new TimeCheckCondition(min, max, period);
  }

  /**
   * Returns a condition matching entity scores against {@code scores}.
   *
   * @param scores the required entity score bounds
   * @return the built condition
   */
  public static Condition entityScores(Map<String, EntityScoresCondition.Bound> scores) {
    return new EntityScoresCondition(Objects.requireNonNull(scores));
  }

  /**
   * Returns a condition matching whether the entity was killed by a player.
   *
   * @param expected the required killed-by-player state
   * @return the built condition
   */
  public static Condition killedByPlayer(boolean expected) {
    return new KilledByPlayerCondition(expected);
  }

  /**
   * Returns a condition applying {@code condition} to the selected entity target.
   *
   * @param target the entity target to select
   * @param condition the condition to apply
   * @return the built condition
   */
  public static Condition targeted(EntityTarget target, Condition condition) {
    return new EntityTargetCondition(
        Objects.requireNonNull(target), Objects.requireNonNull(condition));
  }

  /**
   * Returns a condition checking the entity position against the supplied bounds.
   *
   * @param minX the minimum X-coordinate bound
   * @param maxX the maximum X-coordinate bound
   * @param minY the minimum Y-coordinate bound
   * @param maxY the maximum Y-coordinate bound
   * @param minZ the minimum Z-coordinate bound
   * @param maxZ the maximum Z-coordinate bound
   * @return the built condition
   */
  public static Condition position(
      Double minX, Double maxX, Double minY, Double maxY, Double minZ, Double maxZ) {
    return new PositionCondition(minX, maxX, minY, maxY, minZ, maxZ);
  }

  /**
   * Returns a condition matching the dimension identifier to {@code dimensionKey}.
   *
   * @param dimensionKey the required dimension identifier
   * @return the built condition
   */
  public static Condition dimension(Key dimensionKey) {
    return new DimensionCondition(Objects.requireNonNull(dimensionKey));
  }

  /**
   * Returns a condition checking light-level bounds.
   *
   * @param minLevel the minimum light level
   * @param maxLevel the maximum light level
   * @param minSky the minimum sky light level
   * @param maxSky the maximum sky light level
   * @param minBlock the minimum block light level
   * @param maxBlock the maximum block light level
   * @return the built condition
   */
  public static Condition light(
      Integer minLevel,
      Integer maxLevel,
      Integer minSky,
      Integer maxSky,
      Integer minBlock,
      Integer maxBlock) {
    return new LightCondition(minLevel, maxLevel, minSky, maxSky, minBlock, maxBlock);
  }

  /**
   * Returns a condition matching whether the entity can see the sky.
   *
   * @param expected the required sky-visibility state
   * @return the built condition
   */
  public static Condition canSeeSky(boolean expected) {
    return new CanSeeSkyCondition(expected);
  }

  /**
   * Returns a condition comparing the player's resource value to {@code expected}.
   *
   * @param type the resource type
   * @param operator the comparison operator
   * @param expected the required resource value
   * @return the built condition
   */
  public static Condition playerResource(
      PlayerResourceType type, RelationalOperator operator, double expected) {
    return new PlayerResourceCondition(type, operator, expected);
  }

  /**
   * Returns a condition matching the presence of the effect identified by {@code effectKey}.
   *
   * @param effectKey the required effect identifier
   * @return the built condition
   */
  public static Condition potionPresent(Key effectKey) {
    return new PotionPresentCondition(Objects.requireNonNull(effectKey));
  }

  /**
   * Returns a condition comparing a potion effect's amplifier to {@code expected}.
   *
   * @param effectKey the potion effect identifier
   * @param operator the comparison operator
   * @param expected the required amplifier
   * @return the built condition
   */
  public static Condition potionAmplifier(
      Key effectKey, RelationalOperator operator, int expected) {
    return new PotionAmplifierCondition(Objects.requireNonNull(effectKey), operator, expected);
  }

  /**
   * Returns a condition comparing a potion effect's duration to {@code expected}.
   *
   * @param effectKey the potion effect identifier
   * @param operator the comparison operator
   * @param expected the required duration
   * @return the built condition
   */
  public static Condition potionDuration(
      Key effectKey, RelationalOperator operator, int expected) {
    return new PotionDurationCondition(Objects.requireNonNull(effectKey), operator, expected);
  }

  /**
   * Returns a condition matching the entity's job to {@code jobKey}.
   *
   * @param jobKey the required job key
   * @return the built condition
   */
  public static Condition job(String jobKey) {
    return new JobCondition(Set.of(jobKey));
  }

  /**
   * Returns a condition matching the entity's job to any supplied job key.
   *
   * @param jobKeys the acceptable job keys
   * @return the built condition
   */
  public static Condition jobAny(String... jobKeys) {
    return new JobCondition(Set.of(jobKeys));
  }

  /**
   * Returns a condition matching the entity's team name to {@code teamName}.
   *
   * @param teamName the required team name
   * @return the built condition
   */
  public static Condition team(String teamName) {
    return new TeamCondition(Objects.requireNonNull(teamName));
  }

  /**
   * Returns a condition comparing the entity's ping to {@code milliseconds}.
   *
   * @param operator the comparison operator
   * @param milliseconds the required ping in milliseconds
   * @return the built condition
   */
  public static Condition ping(RelationalOperator operator, int milliseconds) {
    return new PingCondition(operator, milliseconds);
  }

  /**
   * Returns a condition requiring at least {@code minimum} empty inventory slots.
   *
   * @param minimum the minimum number of empty slots
   * @param includeOffhand whether to include the offhand slot
   * @param includeHotbar whether to include hotbar slots
   * @return the built condition
   */
  public static Condition emptySlots(int minimum, boolean includeOffhand, boolean includeHotbar) {
    return new EmptySlotsCondition(minimum, includeOffhand, includeHotbar);
  }

  /**
   * Returns a condition matching sheep shearing and color, allowing nullable criteria.
   *
   * @param sheared the optional sheared state
   * @param color the optional wool color
   * @return the built condition
   */
  public static Condition sheep(@Nullable Boolean sheared, @Nullable Key color) {
    return new SheepCondition(sheared, color);
  }

  /**
   * Returns a condition matching the frog variant to {@code variant}.
   *
   * @param variant the required frog variant
   * @return the built condition
   */
  public static Condition frog(Key variant) {
    return new FrogCondition(Objects.requireNonNull(variant));
  }

  /**
   * Returns a condition matching the cat variant to {@code variant}.
   *
   * @param variant the required cat variant
   * @return the built condition
   */
  public static Condition cat(Key variant) {
    return new CatCondition(Objects.requireNonNull(variant));
  }

  /**
   * Returns a condition requiring at least {@code minimum} items of the material.
   *
   * @param material the required item material
   * @param minimum the minimum item count
   * @return the built condition
   */
  public static Condition requiredItemCount(Key material, int minimum) {
    return new RequiredItemCountCondition(Objects.requireNonNull(material), minimum);
  }

  /**
   * Returns a condition comparing playtime in ticks to {@code ticks}.
   *
   * @param operator the comparison operator
   * @param ticks the required playtime in ticks
   * @return the built condition
   */
  public static Condition playtime(RelationalOperator operator, long ticks) {
    return new PlaytimeCondition(operator, ticks);
  }

  /**
   * Returns a condition requiring an armor set from {@code items}.
   *
   * @param items the required armor item keys
   * @return the built condition
   */
  public static Condition armorSet(Set<Key> items) {
    return new ArmorSetCondition(Objects.requireNonNull(items));
  }

  /**
   * Returns a condition requiring an armor set from the supplied items.
   *
   * @param items the required armor item keys
   * @return the built condition
   */
  public static Condition armorSet(Key... items) {
    return new ArmorSetCondition(Set.of(items));
  }

  /**
   * Returns a condition comparing invulnerability frames to {@code ticks}.
   *
   * @param operator the comparison operator
   * @param ticks the required invulnerability frames
   * @return the built condition
   */
  public static Condition invulnerableFrames(RelationalOperator operator, int ticks) {
    return new InvulnerableFramesCondition(operator, ticks);
  }

  /**
   * Returns a condition matching the advancement identifier to {@code advancement}.
   *
   * @param advancement the required advancement identifier
   * @return the built condition
   */
  public static Condition advancement(Key advancement) {
    return new AdvancementCondition(Objects.requireNonNull(advancement));
  }

  /**
   * Returns a condition applying {@code condition} to the entity's vehicle.
   *
   * @param condition the condition to apply
   * @return the built condition
   */
  public static Condition vehicle(Condition condition) {
    return new VehicleCondition(Objects.requireNonNull(condition));
  }

  /**
   * Returns a condition applying {@code condition} to the entity's passenger.
   *
   * @param condition the condition to apply
   * @return the built condition
   */
  public static Condition passenger(Condition condition) {
    return new PassengerCondition(Objects.requireNonNull(condition));
  }

  /**
   * Returns a condition checking a block property against an inclusive integer range.
   *
   * @param name the block property name
   * @param min the inclusive minimum value
   * @param max the inclusive maximum value
   * @return the built condition
   */
  public static Condition blockPropertyRange(String name, int min, int max) {
    return new BlockPropertyRangeCondition(Objects.requireNonNull(name), min, max);
  }

  /**
   * Returns a condition that is true on periodic ticks using {@code period} and {@code offset}.
   *
   * @param period the tick period
   * @param offset the tick offset
   * @return the built condition
   */
  public static Condition periodicTick(int period, int offset) {
    return new PeriodicTickCondition(period, offset);
  }

  /**
   * Returns a condition that succeeds randomly with the supplied {@code chance}.
   *
   * @param chance the success probability
   * @return the built condition
   */
  public static Condition randomChance(double chance) {
    return new RandomChanceCondition(chance);
  }

  /**
   * Returns a condition requiring every supplied term to match.
   *
   * @param terms the terms that must all match
   * @return the built condition
   */
  public static Condition allOf(Condition... terms) {
    List<Condition> flattened = new ArrayList<>();
    for (Condition term : terms) {
      flattenAll(Objects.requireNonNull(term, "term"), flattened);
    }
    return new AllOfCondition(List.copyOf(flattened));
  }

  /**
   * Returns a condition requiring both supplied terms to match.
   *
   * @param a the first term that must match
   * @param b the second term that must match
   * @return the built condition
   */
  public static Condition allOf(Condition a, Condition b) {
    List<Condition> terms = new ArrayList<>();
    flattenAll(a, terms);
    flattenAll(b, terms);
    return new AllOfCondition(List.copyOf(terms));
  }

  /**
   * Returns a condition requiring at least one supplied term to match.
   *
   * @param terms the terms, at least one of which must match
   * @return the built condition
   */
  public static Condition anyOf(Condition... terms) {
    List<Condition> flattened = new ArrayList<>();
    for (Condition term : terms) {
      flattenAny(Objects.requireNonNull(term, "term"), flattened);
    }
    return new AnyOfCondition(List.copyOf(flattened));
  }

  /**
   * Returns a condition requiring either supplied term to match.
   *
   * @param a the first term
   * @param b the second term
   * @return the built condition
   */
  public static Condition anyOf(Condition a, Condition b) {
    List<Condition> terms = new ArrayList<>();
    flattenAny(a, terms);
    flattenAny(b, terms);
    return new AnyOfCondition(List.copyOf(terms));
  }

  /**
   * Returns a condition that negates {@code term}.
   *
   * @param term the term to negate
   * @return the built condition
   */
  public static Condition inverted(Condition term) {
    return new InvertedCondition(Objects.requireNonNull(term));
  }

  private static void flattenAll(Condition condition, List<Condition> out) {
    if (condition instanceof AllOfCondition all) {
      out.addAll(all.terms());
    } else {
      out.add(condition);
    }
  }

  private static void flattenAny(Condition condition, List<Condition> out) {
    if (condition instanceof AnyOfCondition any) {
      out.addAll(any.terms());
    } else {
      out.add(condition);
    }
  }

  /**
   * Returns a condition applying {@code condition} to the block being stepped on.
   *
   * @param condition the condition to apply
   * @return the built condition
   */
  public static Condition steppingOn(Condition condition) {
    return new SteppingOnCondition(Objects.requireNonNull(condition));
  }

  /**
   * Returns a condition applying {@code condition} to the matched block.
   *
   * @param condition the condition to apply
   * @return the built condition
   */
  public static Condition matchBlock(Condition condition) {
    return new MatchBlockCondition(Objects.requireNonNull(condition));
  }

  /**
   * Returns a condition applying {@code condition} at the supplied location offset.
   *
   * @param offsetX the X-coordinate offset
   * @param offsetY the Y-coordinate offset
   * @param offsetZ the Z-coordinate offset
   * @param condition the condition to apply
   * @return the built condition
   */
  public static Condition locationOffset(
      int offsetX, int offsetY, int offsetZ, Condition condition) {
    return new LocationOffsetCondition(offsetX, offsetY, offsetZ, Objects.requireNonNull(condition));
  }

  /**
   * Returns a condition requiring an offhand item of at least {@code minimumAmount}.
   *
   * @param materials the acceptable item materials
   * @param minimumAmount the minimum item amount
   * @return the built condition
   */
  public static Condition offhandItem(Set<Key> materials, int minimumAmount) {
    return new OffhandItemCondition(materials, minimumAmount, null);
  }

  /**
   * Returns a condition requiring an offhand item, optionally matching custom model data.
   *
   * @param materials the acceptable item materials
   * @param minimumAmount the minimum item amount
   * @param customModelData the optional custom model data
   * @return the built condition
   */
  public static Condition offhandItem(
      Set<Key> materials, int minimumAmount, @Nullable Integer customModelData) {
    return new OffhandItemCondition(
        materials == null ? Set.of() : materials, minimumAmount, customModelData);
  }

  /**
   * Returns a condition checking item counts within the supplied bounds.
   *
   * @param items the acceptable item keys
   * @param minimum the minimum item count
   * @param maximum the maximum item count
   * @return the built condition
   */
  public static Condition itemTypeCount(Set<Key> items, Integer minimum, Integer maximum) {
    return new ItemTypeCountCondition(
        items == null ? Set.of() : items, minimum, maximum);
  }

  /**
   * Returns a condition checking equipment-slot item counts within the supplied bounds.
   *
   * @param slot the equipment slot
   * @param items the acceptable item keys
   * @param minimum the minimum item count
   * @param maximum the maximum item count
   * @return the built condition
   */
  public static Condition equipmentItem(
      EquipmentSlotKey slot, Set<Key> items, Integer minimum, Integer maximum) {
    return new EquipmentItemCondition(
        Objects.requireNonNull(slot), items == null ? Set.of() : items, minimum, maximum);
  }

  /**
   * Returns a condition checking item durability within the supplied bounds.
   *
   * @param minimum the minimum durability
   * @param maximum the maximum durability
   * @return the built condition
   */
  public static Condition itemDurability(Integer minimum, Integer maximum) {
    return new ItemDurabilityCondition(minimum, maximum);
  }

  /**
   * Returns a condition checking item enchantment levels within {@code bounds}.
   *
   * @param bounds the enchantment level bounds
   * @return the built condition
   */
  public static Condition itemEnchantments(Map<Key, ItemEnchantmentsCondition.LevelBound> bounds) {
    return new ItemEnchantmentsCondition(bounds == null ? Map.of() : bounds);
  }

  /**
   * Returns a condition matching optional item trim material and pattern.
   *
   * @param material the optional trim material
   * @param pattern the optional trim pattern
   * @return the built condition
   */
  public static Condition itemTrim(@Nullable Key material, @Nullable Key pattern) {
    return new ItemTrimCondition(material, pattern);
  }

  /**
   * Returns a condition matching optional wolf collar color and variant.
   *
   * @param collarColor the optional collar color
   * @param variant the optional wolf variant
   * @return the built condition
   */
  public static Condition wolf(@Nullable Key collarColor, @Nullable Key variant) {
    return new WolfVariantCondition(collarColor, variant);
  }

  /**
   * Returns a condition matching optional tropical fish appearance properties.
   *
   * @param variant the optional tropical fish variant
   * @param pattern the optional tropical fish pattern
   * @param baseColor the optional base color
   * @param patternColor the optional pattern color
   * @return the built condition
   */
  public static Condition tropicalFish(
      @Nullable Integer variant, @Nullable Key pattern, @Nullable Key baseColor,
      @Nullable Key patternColor) {
    return new TropicalFishCondition(variant, pattern, baseColor, patternColor);
  }

  /**
   * Returns a condition checking optional villager properties and level bounds.
   *
   * @param type the optional villager type
   * @param profession the optional villager profession
   * @param minimumLevel the minimum villager level
   * @param maximumLevel the maximum villager level
   * @return the built condition
   */
  public static Condition villager(
      @Nullable Key type, @Nullable Key profession, Integer minimumLevel, Integer maximumLevel) {
    return new VillagerCondition(type, profession, minimumLevel, maximumLevel);
  }

  /**
   * Returns a condition matching horse color and armor items.
   *
   * @param color the optional horse color
   * @param armorItems the acceptable armor item keys
   * @return the built condition
   */
  public static Condition horse(@Nullable Key color, Set<Key> armorItems) {
    return new HorseCondition(color, armorItems == null ? Set.of() : armorItems);
  }

  /**
   * Returns a condition checking an active cooldown for {@code key}.
   *
   * @param key the cooldown key
   * @param source the optional cooldown source
   * @param active the optional active state
   * @param minimumRemainingTicks the minimum remaining cooldown ticks
   * @return the built condition
   */
  public static Condition activeCooldown(
      Key key,
      @Nullable String source,
      @Nullable Boolean active,
      @Nullable Integer minimumRemainingTicks) {
    return new ActiveCooldownCondition(key, source, active, minimumRemainingTicks);
  }
}
