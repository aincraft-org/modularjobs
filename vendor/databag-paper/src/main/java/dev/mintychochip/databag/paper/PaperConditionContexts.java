package dev.mintychochip.databag.paper;

import dev.mintychochip.databag.condition.OffsetContextResolver;
import dev.mintychochip.databag.condition.ItemSnapshot;
import dev.mintychochip.databag.condition.EquipmentSlotKey;
import dev.mintychochip.databag.condition.TrimSnapshot;
import dev.mintychochip.databag.condition.CooldownSnapshot;
import dev.mintychochip.databag.condition.ItemSubject;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.PotionEffectSnapshot;
import dev.mintychochip.databag.condition.WeatherState;
import dev.mintychochip.databag.DataBag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Cat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a {@link ConditionContext} from live Paper objects.
 *
 * <p>Non-instantiable utility. Factory methods return {@link ConditionContext#absent()} or set
 * {@code present}/{@code livingPresent} to {@code false} when required live state is unavailable,
 * so downstream conditions fail closed on missing snapshot fields.
 */
public final class PaperConditionContexts {

  private PaperConditionContexts() {}

  /**
   * Snapshot of {@code player} with no job keys.
   *
   * <p>Delegates to {@link #from(Player, Set, DataBag)} with an empty job-key set and extras bag.
   *
   * @param player player to snapshot, or {@code null}
   * @return a player context, or {@link ConditionContext#absent()} when {@code player} is
   *     {@code null} or offline
   */
  public static ConditionContext from(@Nullable Player player) {
    return from(player, Set.of());
  }

  /**
   * Snapshot of {@code player} plus optional job keys (namespaced or bare).
   *
   * <p>Delegates to {@link #from(Player, Set, DataBag)} with an empty extras bag.
   *
   * @param player player to snapshot, or {@code null}
   * @param jobKeys job keys to attach, or {@code null} for an empty set
   * @return a player context, or {@link ConditionContext#absent()} when {@code player} is
   *     {@code null} or offline
   */
  public static ConditionContext from(
      @Nullable Player player, @Nullable Set<String> jobKeys) {
    return from(player, jobKeys, DataBag.create());
  }

  /**
   * Snapshot of {@code player} plus job keys and extension {@link DataBag}
   * extras for registered {@link dev.mintychochip.databag.condition.ConditionHandler}s.
   *
   * @param player player to snapshot, or {@code null}
   * @param jobKeys job keys to attach, or {@code null} for an empty set
   * @param extras handler extension data, or {@code null} for an empty bag
   * @return a player context with {@code present} {@code true} and player fields populated from
   *     {@code player}, or {@link ConditionContext#absent()} when {@code player} is {@code null}
   *     or offline
   */
  public static ConditionContext from(
      @Nullable Player player, @Nullable Set<String> jobKeys, @Nullable DataBag extras) {
    if (player == null || !player.isOnline()) {
      return ConditionContext.absent();
    }
    return playerBuilder(player, jobKeys)
        .extras(extras == null ? DataBag.create() : extras)
        .build();
  }

  private static ConditionContext.Builder playerBuilder(
      Player player, @Nullable Set<String> jobKeys) {
    return livingBuilder(player)
        .present(true)
        .entityType(Key.key("minecraft:player"))
        .flying(player.isFlying())
        .gameMode(player.getGameMode().name().toLowerCase(Locale.ROOT))
        .hunger((double) player.getFoodLevel())
        .experience((double) player.getExp())
        .xpLevel((double) player.getLevel())
        .absorption(player.getAbsorptionAmount())
        .airRemaining((double) player.getRemainingAir())
        .scores(playerScores(player))
        .itemCounts(itemCountsOf(player))
        .playtime((long) player.getStatistic(Statistic.PLAY_ONE_MINUTE))
        .armorSet(armorSetOf(player))
        .offhandItem(offhandItemOf(player))
        .activeCooldowns(activeCooldownsOf(player))
        .invulnerableTicks(player.getNoDamageTicks())
        .advancements(advancementsOf(player))
        .emptyMain(emptyMainOf(player))
        .emptyHotbar(emptyHotbarOf(player))
        .emptyOffhand(emptyOffhandOf(player))
        .team(teamOf(player))
        .ping(player.getPing())
        .vehicle(vehicleContext(player))
        .passengers(passengerContexts(player))
        .jobKeys(jobKeys == null ? Set.of() : jobKeys);
  }

  /**
   * Snapshot of a living entity that is not required to be a player.
   *
   * @param entity entity to snapshot, or {@code null}
   * @return a living-entity context with {@code livingPresent} {@code true} and {@code present}
   *     {@code false}, {@link ConditionContext#absent()} when {@code entity} is {@code null} or
   *     dead, or a player snapshot when {@code entity} is a {@link Player}
   */
  public static ConditionContext fromLiving(@Nullable LivingEntity entity) {
    if (entity == null || entity.isDead()) {
      return ConditionContext.absent();
    }
    if (entity instanceof Player player) {
      return from(player);
    }
    return livingBuilder(entity)
        .present(false)
        .flying(false)
        .gameMode(null)
        .vehicle(vehicleContext(entity))
        .passengers(passengerContexts(entity))
        .build();
  }

  /**
   * Snapshot of a death victim with nested attacker contexts.
   *
   * @param victim the entity that died, or {@code null}
   * @param jobKeys job keys for a player victim, or {@code null} for an empty set
   * @param attacker indirect attacker, or {@code null} to omit
   * @param directAttacker direct damage source, or {@code null} to omit
   * @param attackingPlayer player responsible for the death, or {@code null} to omit
   * @return a context with {@code present} and {@code livingPresent} set to {@code false} when
   *     the victim is absent, dead, or an offline player; otherwise a player or living-entity
   *     victim snapshot plus any non-null attacker snapshots from {@link #fromLiving} or
   *     {@link #from(Player)}
   */
  public static ConditionContext fromDeath(
      @Nullable LivingEntity victim,
      @Nullable Set<String> jobKeys,
      @Nullable LivingEntity attacker,
      @Nullable LivingEntity directAttacker,
      @Nullable Player attackingPlayer) {
    ConditionContext.Builder builder;
    if (victim instanceof Player player) {
      if (!player.isOnline()) {
        builder = ConditionContext.builder().present(false).livingPresent(false);
      } else {
        builder = playerBuilder(player, jobKeys);
      }
    } else if (victim == null || victim.isDead()) {
      builder = ConditionContext.builder().present(false).livingPresent(false);
    } else {
      builder = livingBuilder(victim)
          .present(false)
          .flying(false)
          .gameMode(null)
          .vehicle(vehicleContext(victim))
          .passengers(passengerContexts(victim));
    }
    builder.death(true);
    if (attacker != null) {
      builder.attacker(fromLiving(attacker));
    }
    if (directAttacker != null) {
      builder.directAttacker(fromLiving(directAttacker));
    }
    if (attackingPlayer != null) {
      builder.attackingPlayer(from(attackingPlayer));
    }
    return builder.build();
  }

  private static @Nullable ConditionContext standingOnOf(LivingEntity entity) {
    Block block = entity.getLocation().getBlock();
    return fromBlock(block.getRelative(BlockFace.DOWN));
  }
  /**
   * Snapshot of a block (id + block-state properties) and its location weather.
   *
   * @param block block to snapshot, or {@code null}
   * @return a block context with {@code present} and {@code livingPresent} set to {@code false},
   *     block id and properties populated, or {@link ConditionContext#absent()} when
   *     {@code block} is {@code null}
   */
  public static ConditionContext fromBlock(@Nullable Block block) {
    if (block == null) {
      return ConditionContext.absent();
    }
    World world = block.getWorld();
    Location loc = block.getLocation();
    return ConditionContext.builder()
        .present(false)
        .livingPresent(false)
        .blockId(block.getType().getKey())
        .blockProperties(BlockDataStrings.properties(block.getBlockData().getAsString()))
        .worldName(world.getName())
        .worldKey(world.getKey())
        .biome(world.getBiome(loc).getKey())
        .weather(weatherOf(world))
        .x(loc.getX())
        .y(loc.getY())
        .z(loc.getZ())
        .lightLevel((int) block.getLightLevel())
        .skyLight((int) block.getLightFromSky())
        .blockLight((int) block.getLightFromBlocks())
        .canSeeSky(block.getLightFromSky() >= 15)
        .dayTime(world.getFullTime())
        .offsetResolver(offsetResolverOf(world, loc))
        .build();
  }

  private static ConditionContext.Builder livingBuilder(LivingEntity entity) {
    World world = entity.getWorld();
    Location loc = entity.getLocation();
    Block block = loc.getBlock();
    Key fluid = entity.isInWater()
        ? Key.key("minecraft:water")
        : entity.isInLava() ? Key.key("minecraft:lava") : null;
    Map<Key, PotionEffectSnapshot> effects = new HashMap<>();
    for (PotionEffect effect : entity.getActivePotionEffects()) {
      effects.put(
          effect.getType().getKey(),
          new PotionEffectSnapshot(effect.getAmplifier(), effect.getDuration()));
    }
    boolean baby = entity instanceof Ageable ageable && !ageable.isAdult();
    ConditionContext.Builder builder = ConditionContext.builder()
        .livingPresent(true)
        .entityType(entity.getType().getKey())
        .sneaking(entity.isSneaking())
        .sprinting(entity instanceof Player player && player.isSprinting())
        .onFire(entity.getFireTicks() > 0)
        .onGround(entity.isOnGround())
        .swimming(entity.isSwimming())
        .baby(baby)
        .gliding(entity.isGliding())
        .biome(world.getBiome(loc).getKey())
        .worldName(world.getName())
        .worldKey(world.getKey())
        .weather(weatherOf(world))
        .fluid(fluid)
        .health(entity.getHealth())
        .effects(effects)
        .x(loc.getX())
        .y(loc.getY())
        .z(loc.getZ())
        .lightLevel((int) block.getLightLevel())
        .skyLight((int) block.getLightFromSky())
        .blockLight((int) block.getLightFromBlocks())
        .canSeeSky(block.getLightFromSky() >= 15)
        .invulnerableTicks(entity.getNoDamageTicks())
        .ticksLived(entity.getTicksLived())
        .random(new java.util.Random(ThreadLocalRandom.current().nextLong()))
        .offsetResolver(offsetResolverOf(world, loc))
        .standingOn(standingOnOf(entity))
        .dayTime(world.getFullTime());
    Map<EquipmentSlotKey, ItemSubject> equipment = equipmentOf(entity);
    builder.equipment(equipment);
    builder.itemSubject(equipment.getOrDefault(EquipmentSlotKey.MAINHAND, ItemSubject.empty()));
    if (entity instanceof Sheep sheep) {
      builder
          .sheared(sheep.isSheared())
          .woolColor(Key.key("minecraft:" + sheep.getColor().name().toLowerCase(Locale.ROOT)));
    }
    if (entity instanceof Frog frog) {
      builder.frogVariant(Key.key(frog.getVariant().getKey().toString()));
    }
    if (entity instanceof Cat cat) {
      builder.catVariant(Key.key(cat.getCatType().getKey().toString()));
    }
    if (entity instanceof org.bukkit.entity.Wolf wolf) {
      builder.wolfVariant(Key.key(wolf.getVariant().getKey().toString()));
      if (wolf.isTamed()) {
        builder.collarColor(
            Key.key("minecraft:" + wolf.getCollarColor().name().toLowerCase(Locale.ROOT)));
      }
    }
    if (entity instanceof org.bukkit.entity.TropicalFish fish) {
      builder
          .tropicalFishPattern(Key.key("minecraft:"
              + fish.getPattern().name().toLowerCase(Locale.ROOT)))
          .tropicalFishBaseColor(Key.key("minecraft:"
              + fish.getBodyColor().name().toLowerCase(Locale.ROOT)))
          .tropicalFishPatternColor(Key.key("minecraft:"
              + fish.getPatternColor().name().toLowerCase(Locale.ROOT)))
          .tropicalFishVariant(
              fish.getPattern().ordinal()
                  | (fish.getBodyColor().ordinal() << 8)
                  | (fish.getPatternColor().ordinal() << 16));
    }
    if (entity instanceof org.bukkit.entity.Villager villager) {
      builder
          .villagerType(Key.key(villager.getVillagerType().getKey().toString()))
          .villagerProfession(Key.key(villager.getProfession().getKey().toString()))
          .villagerLevel(villager.getVillagerLevel());
    }
    if (entity instanceof org.bukkit.entity.Horse horse) {
      builder.horseColor(
          Key.key("minecraft:" + horse.getColor().name().toLowerCase(Locale.ROOT)));
      builder.horseArmor(subjectOf(horse.getInventory().getArmor()));
    }
    return builder;
  }

  private static int emptyMainOf(Player player) {
    org.bukkit.inventory.PlayerInventory inv = player.getInventory();
    int empty = 0;
    org.bukkit.inventory.ItemStack[] contents = inv.getStorageContents();
    for (int i = 9; i < contents.length && i < 36; i++) {
      org.bukkit.inventory.ItemStack stack = contents[i];
      if (stack == null || stack.getType().isAir()) {
        empty++;
      }
    }
    return empty;
  }

  private static int emptyHotbarOf(Player player) {
    org.bukkit.inventory.PlayerInventory inv = player.getInventory();
    int empty = 0;
    org.bukkit.inventory.ItemStack[] contents = inv.getStorageContents();
    for (int i = 0; i < 9 && i < contents.length; i++) {
      org.bukkit.inventory.ItemStack stack = contents[i];
      if (stack == null || stack.getType().isAir()) {
        empty++;
      }
    }
    return empty;
  }

  private static int emptyOffhandOf(Player player) {
    org.bukkit.inventory.ItemStack stack = player.getInventory().getItemInOffHand();
    return (stack == null || stack.getType().isAir()) ? 1 : 0;
  }



  private static Map<EquipmentSlotKey, ItemSubject> equipmentOf(LivingEntity entity) {
    Map<EquipmentSlotKey, ItemSubject> equipment = new HashMap<>();
    org.bukkit.inventory.EntityEquipment slots = entity.getEquipment();
    if (slots == null) {
      for (EquipmentSlotKey slot : EquipmentSlotKey.values()) {
        equipment.put(slot, ItemSubject.empty());
      }
      return equipment;
    }
    equipment.put(EquipmentSlotKey.MAINHAND, subjectOf(slots.getItemInMainHand()));
    equipment.put(EquipmentSlotKey.OFFHAND, subjectOf(slots.getItemInOffHand()));
    equipment.put(EquipmentSlotKey.HEAD, subjectOf(slots.getHelmet()));
    equipment.put(EquipmentSlotKey.CHEST, subjectOf(slots.getChestplate()));
    equipment.put(EquipmentSlotKey.LEGS, subjectOf(slots.getLeggings()));
    equipment.put(EquipmentSlotKey.FEET, subjectOf(slots.getBoots()));
    return equipment;
  }

  private static ItemSubject subjectOf(@Nullable org.bukkit.inventory.ItemStack stack) {
    if (stack == null || stack.getType().isAir()) {
      return ItemSubject.empty();
    }
    Integer maxDurability = null;
    Integer damage = null;
    Map<Key, Integer> enchantments = new HashMap<>();
    TrimSnapshot trim = null;
    if (stack.hasItemMeta()) {
      org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
      if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
        if (damageable.hasMaxDamage()) {
          maxDurability = damageable.getMaxDamage();
        }
        if (damageable.hasDamage()) {
          damage = damageable.getDamage();
        }
      }
      if (meta instanceof org.bukkit.inventory.meta.ArmorMeta armorMeta
          && armorMeta.hasTrim()) {
        org.bukkit.inventory.meta.trim.ArmorTrim armorTrim = armorMeta.getTrim();
        trim = new TrimSnapshot(
            Key.key(armorTrim.getMaterial().getKey().toString()),
            Key.key(armorTrim.getPattern().getKey().toString()));
      }
      for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry
          : meta.getEnchants().entrySet()) {
        enchantments.put(
            Key.key(entry.getKey().getKey().toString()),
            entry.getValue());
      }
    }
    return new ItemSubject(
        Key.key(stack.getType().getKey().toString()),
        stack.getAmount(),
        maxDurability,
        damage,
        enchantments,
        trim);
  }

  private static final org.bukkit.Material[] ITEM_MATERIALS = itemMaterials();

  private static org.bukkit.Material[] itemMaterials() {
    return java.util.Arrays.stream(org.bukkit.Material.values())
        .filter(org.bukkit.Material::isItem)
        .toArray(org.bukkit.Material[]::new);
  }

  private static Set<CooldownSnapshot> activeCooldownsOf(Player player) {
    Set<CooldownSnapshot> cooldowns = new HashSet<>();
    for (org.bukkit.Material material : ITEM_MATERIALS) {
      int ticks = player.getCooldown(material);
      if (ticks > 0) {
        cooldowns.add(new CooldownSnapshot(
            Key.key(material.getKey().toString()), "paper", ticks));
      }
    }
    return cooldowns;
  }
  private static ItemSnapshot offhandItemOf(Player player) {
    org.bukkit.inventory.ItemStack stack = player.getInventory().getItemInOffHand();
    if (stack == null || stack.getType().isAir()) {
      return ItemSnapshot.empty();
    }
    Integer customModelData = null;
    String displayName = null;
    List<String> lore = List.of();
    if (stack.hasItemMeta()) {
      org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
      if (meta.hasCustomModelData()) {
        customModelData = meta.getCustomModelData();
      }
      if (meta.hasDisplayName()) {
        displayName = meta.getDisplayName();
      }
      if (meta.hasLore()) {
        lore = List.copyOf(meta.getLore());
      }
    }
    return new ItemSnapshot(
        Key.key(stack.getType().getKey().toString()),
        stack.getAmount(),
        customModelData,
        displayName,
        lore);
  }
  private static Set<Key> armorSetOf(Player player) {
    Set<Key> set = new HashSet<>();
    for (org.bukkit.inventory.ItemStack stack : player.getInventory().getArmorContents()) {
      if (stack == null) {
        continue;
      }
      if (stack.getType().isAir()) {
        continue;
      }
      set.add(Key.key(stack.getType().getKey().toString()));
    }
    return set;
  }

  private static Map<Key, Integer> itemCountsOf(Player player) {
    Map<Key, Integer> counts = new HashMap<>();
    org.bukkit.inventory.PlayerInventory inv = player.getInventory();
    java.util.function.Consumer<org.bukkit.inventory.ItemStack> add = stack -> {
      if (stack == null) {
        return;
      }
      if (stack.getType().isAir()) {
        return;
      }
      Key key = Key.key(stack.getType().getKey().toString());
      counts.merge(key, stack.getAmount(), Integer::sum);
    };
    for (org.bukkit.inventory.ItemStack stack : inv.getStorageContents()) {
      add.accept(stack);
    }
    for (org.bukkit.inventory.ItemStack stack : inv.getArmorContents()) {
      add.accept(stack);
    }
    add.accept(inv.getItemInOffHand());
    return counts;
  }

  private static Set<String> advancementsOf(Player player) {
    Set<String> completed = new HashSet<>();
    for (Iterator<Advancement> it = Bukkit.getServer().advancementIterator(); it.hasNext(); ) {
      Advancement adv = it.next();
      if (player.getAdvancementProgress(adv).isDone()) {
        completed.add(adv.getKey().toString());
      }
    }
    return completed;
  }

  private static @Nullable ConditionContext vehicleContext(LivingEntity entity) {
    org.bukkit.entity.Entity v = entity.getVehicle();
    if (v instanceof LivingEntity living) {
      return livingBuilder(living).build();
    }
    return null;
  }

  private static List<ConditionContext> passengerContexts(LivingEntity entity) {
    List<ConditionContext> list = new ArrayList<>();
    for (org.bukkit.entity.Entity p : entity.getPassengers()) {
      if (p instanceof LivingEntity living) {
        list.add(livingBuilder(living).build());
      }
    }
    return list;
  }

  private static @Nullable String teamOf(Player player) {
    Scoreboard board = player.getScoreboard();
    Team team = board.getEntryTeam(player.getName());
    return team == null ? null : team.getName();
  }

  private static Map<String, Integer> playerScores(Player player) {
    try {
      Scoreboard scoreboard = player.getScoreboard();
      Map<String, Integer> scores = new HashMap<>();
      for (Objective objective : scoreboard.getObjectives()) {
        Score score = objective.getScore(player.getName());
        if (score.isScoreSet()) {
          scores.put(objective.getName(), score.getScore());
        }
      }
      return scores;
    } catch (RuntimeException ignored) {
      return Map.of();
    }
  }


  private static OffsetContextResolver offsetResolverOf(World world, Location location) {
    return (offsetX, offsetY, offsetZ) ->
        fromBlock(world.getBlockAt(
            location.getBlockX() + offsetX,
            location.getBlockY() + offsetY,
            location.getBlockZ() + offsetZ));
  }
  private static WeatherState weatherOf(World world) {
    return world.isThundering()
        ? WeatherState.THUNDERING
        : world.hasStorm() ? WeatherState.RAINING : WeatherState.CLEAR;
  }

}
