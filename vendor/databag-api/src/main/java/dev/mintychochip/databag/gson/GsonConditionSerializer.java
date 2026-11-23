package dev.mintychochip.databag.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import dev.mintychochip.databag.condition.builtin.AdvancementCondition;
import dev.mintychochip.databag.condition.builtin.AllOfCondition;
import dev.mintychochip.databag.condition.builtin.AlwaysCondition;
import dev.mintychochip.databag.condition.builtin.AnyOfCondition;
import dev.mintychochip.databag.condition.builtin.ArmorSetCondition;
import dev.mintychochip.databag.condition.builtin.BabyCondition;
import dev.mintychochip.databag.condition.builtin.BiomeCondition;
import dev.mintychochip.databag.condition.builtin.BlockPropertyRangeCondition;
import dev.mintychochip.databag.condition.builtin.BlockIdCondition;
import dev.mintychochip.databag.condition.builtin.BlockPropertyCondition;
import dev.mintychochip.databag.condition.builtin.CanSeeSkyCondition;
import dev.mintychochip.databag.condition.builtin.CatCondition;
import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionHandler;
import dev.mintychochip.databag.condition.ConditionHandlers;
import dev.mintychochip.databag.condition.ConditionSerializer;
import dev.mintychochip.databag.condition.Conditions;
import dev.mintychochip.databag.DataBag;
import dev.mintychochip.databag.condition.builtin.DimensionCondition;
import dev.mintychochip.databag.condition.builtin.EmptySlotsCondition;
import dev.mintychochip.databag.condition.builtin.EntityScoresCondition;
import dev.mintychochip.databag.condition.EntityTarget;
import dev.mintychochip.databag.condition.builtin.LocationOffsetCondition;
import dev.mintychochip.databag.condition.builtin.MatchBlockCondition;
import dev.mintychochip.databag.condition.builtin.OffhandItemCondition;
import dev.mintychochip.databag.condition.builtin.EquipmentItemCondition;
import dev.mintychochip.databag.condition.EquipmentSlotKey;
import dev.mintychochip.databag.condition.builtin.ItemTypeCountCondition;
import dev.mintychochip.databag.condition.builtin.ItemDurabilityCondition;
import dev.mintychochip.databag.condition.builtin.ItemEnchantmentsCondition;
import dev.mintychochip.databag.condition.builtin.ItemTrimCondition;
import dev.mintychochip.databag.condition.builtin.HorseCondition;
import dev.mintychochip.databag.condition.builtin.TropicalFishCondition;
import dev.mintychochip.databag.condition.builtin.VillagerCondition;
import dev.mintychochip.databag.condition.builtin.WolfVariantCondition;
import dev.mintychochip.databag.condition.builtin.ActiveCooldownCondition;
import dev.mintychochip.databag.condition.CooldownSnapshot;
import dev.mintychochip.databag.condition.builtin.EntityTargetCondition;
import dev.mintychochip.databag.condition.builtin.EntityTypeCondition;
import dev.mintychochip.databag.condition.builtin.FluidCondition;
import dev.mintychochip.databag.condition.builtin.FrogCondition;
import dev.mintychochip.databag.condition.builtin.FlyingCondition;
import dev.mintychochip.databag.condition.builtin.GameModeCondition;
import dev.mintychochip.databag.condition.builtin.GlidingCondition;
import dev.mintychochip.databag.condition.builtin.InvertedCondition;
import dev.mintychochip.databag.condition.builtin.InvulnerableFramesCondition;
import dev.mintychochip.databag.condition.builtin.JobCondition;
import dev.mintychochip.databag.condition.builtin.KilledByPlayerCondition;
import dev.mintychochip.databag.condition.builtin.LightCondition;
import dev.mintychochip.databag.condition.builtin.OnFireCondition;
import dev.mintychochip.databag.condition.builtin.OnGroundCondition;
import dev.mintychochip.databag.condition.builtin.PlaytimeCondition;
import dev.mintychochip.databag.condition.builtin.PlayerResourceCondition;
import dev.mintychochip.databag.condition.PlayerResourceType;
import dev.mintychochip.databag.condition.builtin.PotionAmplifierCondition;
import dev.mintychochip.databag.condition.builtin.PassengerCondition;
import dev.mintychochip.databag.condition.builtin.PeriodicTickCondition;
import dev.mintychochip.databag.condition.builtin.RandomChanceCondition;
import dev.mintychochip.databag.condition.builtin.PingCondition;
import dev.mintychochip.databag.condition.builtin.VehicleCondition;
import dev.mintychochip.databag.condition.builtin.RequiredItemCountCondition;
import dev.mintychochip.databag.condition.builtin.PotionDurationCondition;
import dev.mintychochip.databag.condition.builtin.PotionPresentCondition;
import dev.mintychochip.databag.condition.builtin.PositionCondition;
import dev.mintychochip.databag.condition.RelationalOperator;
import dev.mintychochip.databag.condition.builtin.SheepCondition;
import dev.mintychochip.databag.condition.builtin.SteppingOnCondition;
import dev.mintychochip.databag.condition.builtin.SneakingCondition;
import dev.mintychochip.databag.condition.builtin.TeamCondition;
import dev.mintychochip.databag.condition.builtin.SprintingCondition;
import dev.mintychochip.databag.condition.builtin.SwimmingCondition;
import dev.mintychochip.databag.condition.builtin.TimeCheckCondition;
import dev.mintychochip.databag.condition.builtin.WeatherCondition;
import dev.mintychochip.databag.condition.WeatherState;
import dev.mintychochip.databag.condition.builtin.WorldCondition;
import net.kyori.adventure.key.Key;

import static dev.mintychochip.databag.gson.ConditionJsonSupport.anyOfConditions;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.conditionId;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.firstString;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.liquidKey;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.namespaceOf;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.operatorName;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.parseOperator;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.parseResource;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.requiredString;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.stringList;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.stripNamespace;
import static dev.mintychochip.databag.gson.ConditionJsonSupport.throwIfUnsupportedVanilla;

/**
 * Vanilla loot-condition JSON reader/writer. Bytes are UTF-8 JSON objects
 * with a {@code condition} id (or legacy {@code type} bag on read).
 */
public final class GsonConditionSerializer implements ConditionSerializer {

  private static final GsonConditionSerializer INSTANCE = new GsonConditionSerializer();

  private GsonConditionSerializer() {}

  /**
   * Returns the singleton Gson condition serializer.
   *
   * @return the shared condition serializer
   */
  public static ConditionSerializer gson() {
    return INSTANCE;
  }

  @Override
  public byte[] write(Condition condition) {
    return writeJson(condition).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String writeJson(Condition condition) {
    return writeElement(condition).toString();
  }

  @Override
  public Condition read(byte[] bytes) {
    return readJson(new String(bytes, StandardCharsets.UTF_8));
  }

  @Override
  public Condition readJson(String json) {
    JsonElement element = JsonParser.parseString(json);
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException("condition JSON must be an object");
    }
    return readObject(element.getAsJsonObject());
  }

  JsonElement writeElement(Condition condition) {
    return switch (condition) {
      case EntityTargetCondition targeted -> writeTargeted(targeted);
      case AlwaysCondition ignored -> allOfJson(List.of());
      case AllOfCondition all -> all.terms().stream().allMatch(EquipmentItemCondition.class::isInstance)
          ? equipmentAllOfJson(all.terms().stream()
              .map(EquipmentItemCondition.class::cast)
              .toList())
          : all.terms().isEmpty()
              ? allOfJson(List.of())
              : allOfJson(all.terms().stream().map(this::writeElement).toList());
      case AnyOfCondition any -> anyOfJson(any.terms().stream().map(this::writeElement).toList());
      case InvertedCondition inverted -> invertedJson(writeElement(inverted.term()));
      case SneakingCondition sneak -> entityFlags(flagObject("is_sneaking", sneak.expected()));
      case SprintingCondition sprint -> entityFlags(flagObject("is_sprinting", sprint.expected()));
      case OnFireCondition fire -> entityFlags(flagObject("is_on_fire", fire.expected()));
      case OnGroundCondition ground -> entityFlags(flagObject("is_on_ground", ground.expected()));
      case SwimmingCondition swim -> entityFlags(flagObject("is_swimming", swim.expected()));
      case BabyCondition baby -> entityFlags(flagObject("is_baby", baby.expected()));
      case GlidingCondition glide -> entityFlags(flagObject("is_gliding", glide.expected()));
      case FlyingCondition fly -> entityFlags(flagObject("is_flying", fly.expected()));
      case EntityTypeCondition type -> entityTypeJson(type.entityType().asString());
      case GameModeCondition mode -> gameModeJson(mode.gameMode());
      case BlockIdCondition block -> blockStateJson(block.blockId().asString(), null, null);
      case BlockPropertyCondition prop -> blockStateJson(null, prop.name(), prop.value());
      case BiomeCondition biome -> entityLocationBiome(biome.biomeKey().asString());
      case WorldCondition world -> modular("modularjobs:world", obj -> obj.addProperty("world", world.worldName()));
      case WeatherCondition weather -> weatherJson(weather.state());
      case FluidCondition fluid -> locationFluid(fluid.fluidKey().asString());
      case PlayerResourceCondition resource -> playerResourceJson(resource);
      case PotionPresentCondition potion -> effectRoot(potion.effectKey().asString(), new JsonObject());
      case PotionAmplifierCondition potion -> effectJson(
          potion.effectKey().asString(), "amplifier", potion.operator(), potion.expected());
      case PotionDurationCondition potion -> effectJson(
          potion.effectKey().asString(), "duration", potion.operator(), potion.expected());
      case JobCondition job -> jobJson(job);
      case TimeCheckCondition time -> timeCheckJson(time);
      case EntityScoresCondition scores -> entityScoresJson(scores);
      case KilledByPlayerCondition killed -> killedByPlayerJson(killed);
      case PositionCondition position -> positionLocationCheckJson(position);
      case DimensionCondition dimension -> dimensionLocationCheckJson(dimension);
      case LightCondition light -> lightLocationCheckJson(light);
      case CanSeeSkyCondition canSeeSky -> canSeeSkyLocationCheckJson(canSeeSky);
      case TeamCondition team -> teamJson(team);
      case PingCondition ping -> pingJson(ping);
      case EmptySlotsCondition empty -> emptySlotsJson(empty);
      case SheepCondition sheep -> sheepJson(sheep);
      case FrogCondition frog -> frogJson(frog);
      case CatCondition cat -> catJson(cat);
      case RequiredItemCountCondition required -> requiredItemCountJson(required);
      case PlaytimeCondition playtime -> playtimeJson(playtime);
      case ArmorSetCondition armor -> armorSetJson(armor);
      case InvulnerableFramesCondition inv -> invulnerableFramesJson(inv);
      case AdvancementCondition adv -> advancementJson(adv);
      case VehicleCondition vehicle -> vehicleJson(vehicle);
      case PassengerCondition passenger -> passengerJson(passenger);
      case BlockPropertyRangeCondition range -> blockPropertyRangeJson(range);
      case PeriodicTickCondition tick -> periodicTickJson(tick);
      case RandomChanceCondition chance -> randomChanceJson(chance);
      case MatchBlockCondition match -> matchBlockJson(match);
      case SteppingOnCondition stepping -> steppingOnJson(stepping);
      case LocationOffsetCondition offset -> locationOffsetJson(offset);
      case OffhandItemCondition offhand -> offhandItemJson(offhand);
      case ItemTypeCountCondition count -> itemTypeCountJson(count);
      case EquipmentItemCondition equipment -> equipmentItemJson(equipment);
      case ItemDurabilityCondition durability -> itemDurabilityJson(durability);
      case ItemEnchantmentsCondition enchantments -> itemEnchantmentsJson(enchantments);
      case ItemTrimCondition trim -> itemTrimJson(trim);
      case WolfVariantCondition wolf -> typeSpecificJson("wolf", wolfJson(wolf));
      case TropicalFishCondition fish -> typeSpecificJson("tropical_fish", tropicalFishJson(fish));
      case VillagerCondition villager -> typeSpecificJson("villager", villagerJson(villager));
      case HorseCondition horse -> typeSpecificJson("horse", horseJson(horse));
      case ActiveCooldownCondition cooldown -> activeCooldownJson(cooldown);
      default -> writeRegistered(condition);
    };
  }

  private JsonElement writeTargeted(EntityTargetCondition targeted) {
    JsonElement inner = writeElement(targeted.condition());
    if (targeted.target() == EntityTarget.THIS) {
      return inner;
    }
    if (!inner.isJsonObject()) {
      throw new IllegalArgumentException("Targeted condition inner JSON must be an object");
    }
    JsonObject object = inner.getAsJsonObject();
    String condition = object.has("condition") ? object.get("condition").getAsString() : "";
    if (!"minecraft:entity_properties".equals(condition)
        && !"minecraft:entity_scores".equals(condition)) {
      throw new IllegalArgumentException(
          "Targeted condition inner JSON must be entity_properties or entity_scores");
    }
    object.addProperty("entity", targeted.target().jsonName());
    return object;
  }

  private JsonObject writeRegistered(Condition condition) {
    ConditionHandler handler = ConditionHandlers.findWriter(condition).orElseThrow(
        () -> new IllegalArgumentException(
            "Cannot serialize condition type: " + condition.getClass().getName()));
    DataBag arguments = handler.write(condition).orElseThrow();
    JsonObject root = new JsonObject();
    root.addProperty("condition", handler.id().asString());
    ArgumentBags.writeFields(root, arguments);
    return root;
  }

  private Condition readObject(JsonObject json) {
    String id = conditionId(json);
    String namespace = namespaceOf(id);
    boolean builtInNamespace = namespace.isEmpty()
        || "minecraft".equals(namespace)
        || "modularjobs".equals(namespace);
    if (!builtInNamespace) {
      return readRegistered(id, json);
    }
    String normalized = stripNamespace(id).toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "all_of", "and" -> readAllOf(json);
      case "any_of", "or" -> readAnyOf(json);
      case "inverted", "not" -> readInverted(json);
      case "entity_properties" -> readEntityProperties(json);
      case "block_state_property" -> readBlockStateProperty(json);
      case "weather_check", "weather" -> readWeather(json);
      case "location_check" -> readLocationCheck(json);
      case "world" -> Conditions.world(requiredString(json, json.has("world") ? "world" : "value"));
      case "player_resource" -> readPlayerResource(json);
      case "job" -> readJob(json);
      case "always" -> Conditions.always();
      case "sneaking" -> Conditions.sneaking(json.get("value").getAsBoolean());
      case "sprinting" -> Conditions.sprinting(json.get("value").getAsBoolean());
      case "biome" -> Conditions.biome(Key.key(requiredString(json, "value")));
      case "liquid" -> Conditions.fluid(Key.key(liquidKey(json)));
      case "potion_effect" -> readLegacyPotion(json);
      case "time_check" -> readTimeCheck(json);
      case "entity_scores" -> readEntityScores(json);
      case "killed_by_player" -> readKilledByPlayer(json);
      case "ping" -> readPing(json);
      case "empty_slots" -> readEmptySlots(json);
      case "required_item_count" -> readRequiredItemCount(json);
      case "playtime" -> readPlaytime(json);
      case "armor_set" -> readArmorSet(json);
      case "invulnerable_frames" -> readInvulnerableFrames(json);
      case "advancement" -> readAdvancement(json);
      case "block_state_range" -> readBlockStateRange(json);
      case "periodic_tick" -> readPeriodicTick(json);
      case "random_chance" -> readRandomChance(json);
      case "match_block" -> readMatchBlock(json);
      case "inventory_offhand_item" -> readOffhandItem(json);
      case "item_type_count" -> readItemTypeCount(json);
      case "item_durability" -> readItemDurability(json);
      case "item_enchantments" -> readItemEnchantments(json);
      case "item_trim" -> readItemTrim(json);
      case "active_cooldown" -> readActiveCooldown(json);
      default -> {
        throwIfUnsupportedVanilla(id, normalized);
        yield readRegistered(id, json);
      }
    };
  }

  private Condition readRegistered(String id, JsonObject json) {
    ConditionHandler handler = ConditionHandlers.get(id).orElseThrow(
        () -> new IllegalArgumentException("Unknown condition type: " + id));
    return handler.read(ArgumentBags.fromJson(json));
  }

  private Condition readAllOf(JsonObject json) {
    List<Condition> terms = readTerms(json, json.has("terms") ? "terms" : "conditions");
    if (terms.isEmpty()) {
      return Conditions.always();
    }
    if (terms.size() == 1) {
      return terms.getFirst();
    }
    return Conditions.allOf(terms.toArray(Condition[]::new));
  }

  private Condition readAnyOf(JsonObject json) {
    List<Condition> terms = readTerms(json, json.has("terms") ? "terms" : "conditions");
    if (terms.size() == 1) {
      return terms.getFirst();
    }
    return Conditions.anyOf(terms.toArray(Condition[]::new));
  }

  private Condition readInverted(JsonObject json) {
    JsonElement inner = json.get("term");
    if (inner == null || !inner.isJsonObject()) {
      throw new IllegalArgumentException("inverted condition requires 'term'");
    }
    return Conditions.inverted(readObject(inner.getAsJsonObject()));
  }

  private Condition readEntityProperties(JsonObject json) {
    EntityTarget target = readEntityTarget(json);
    JsonObject predicate = json.has("predicate") && json.get("predicate").isJsonObject()
        ? json.getAsJsonObject("predicate")
        : json;
    List<Condition> parts = new ArrayList<>();
    if (predicate.has("type") && predicate.get("type").isJsonPrimitive()) {
      parts.add(Conditions.entityType(Key.key(predicate.get("type").getAsString())));
    }
    if (predicate.has("flags") && predicate.get("flags").isJsonObject()) {
      JsonObject flags = predicate.getAsJsonObject("flags");
      addFlag(parts, flags, "is_sneaking", Conditions::sneaking);
      addFlag(parts, flags, "is_sprinting", Conditions::sprinting);
      addFlag(parts, flags, "is_on_fire", Conditions::onFire);
      addFlag(parts, flags, "is_on_ground", Conditions::onGround);
      addFlag(parts, flags, "is_swimming", Conditions::swimming);
      addFlag(parts, flags, "is_baby", Conditions::baby);
      addFlag(parts, flags, "is_gliding", Conditions::gliding);
      addFlag(parts, flags, "is_flying", Conditions::flying);
    }
    if (predicate.has("type_specific") && predicate.get("type_specific").isJsonObject()) {
      JsonObject specific = predicate.getAsJsonObject("type_specific");
      if (specific.has("gamemode")) {
        Condition modes = anyOfConditions(stringList(specific.get("gamemode")).stream()
            .map(Conditions::gameMode)
            .toList());
        if (modes != null) {
          parts.add(modes);
        }
      }
      if (specific.has("sheep") && specific.get("sheep").isJsonObject()) {
        parts.add(readSheep(specific.getAsJsonObject("sheep")));
      }
      if (specific.has("frog") && specific.get("frog").isJsonObject()) {
        parts.add(readFrog(specific.getAsJsonObject("frog")));
      }
      if (specific.has("cat") && specific.get("cat").isJsonObject()) {
        parts.add(readCat(specific.getAsJsonObject("cat")));
      }
      if (specific.has("wolf") && specific.get("wolf").isJsonObject()) {
        parts.add(readWolf(specific.getAsJsonObject("wolf")));
      }
      if (specific.has("tropical_fish") && specific.get("tropical_fish").isJsonObject()) {
        parts.add(readTropicalFish(specific.getAsJsonObject("tropical_fish")));
      }
      if (specific.has("villager") && specific.get("villager").isJsonObject()) {
        parts.add(readVillager(specific.getAsJsonObject("villager")));
      }
      if (specific.has("horse") && specific.get("horse").isJsonObject()) {
        parts.add(readHorse(specific.getAsJsonObject("horse")));
      }
    }
    if (predicate.has("passenger") && predicate.get("passenger").isJsonObject()) {
      parts.add(Conditions.passenger(readEntityProperties(predicate.getAsJsonObject("passenger"))));
    }
    if (predicate.has("vehicle") && predicate.get("vehicle").isJsonObject()) {
      parts.add(Conditions.vehicle(readEntityProperties(predicate.getAsJsonObject("vehicle"))));
    }
    if (predicate.has("stepping_on") && predicate.get("stepping_on").isJsonObject()) {
      parts.add(readSteppingOn(predicate.getAsJsonObject("stepping_on")));
    }
    if (predicate.has("equipment") && predicate.get("equipment").isJsonObject()) {
      JsonObject slots = predicate.getAsJsonObject("equipment");
      for (var entry : slots.entrySet()) {
        parts.add(readEquipmentSlot(EquipmentSlotKey.fromJson(entry.getKey()), entry.getValue()));
      }
    }
    if (predicate.has("location") && predicate.get("location").isJsonObject()) {
      JsonObject location = predicate.getAsJsonObject("location");
      if (location.has("biomes")) {
        Condition biomes = anyOfConditions(stringList(location.get("biomes")).stream()
            .map(biome -> Conditions.biome(Key.key(biome)))
            .toList());
        if (biomes != null) {
          parts.add(biomes);
        }
      }
    }
    if (predicate.has("team") && predicate.get("team").isJsonPrimitive()) {
      parts.add(Conditions.team(predicate.get("team").getAsString()));
    }
    if (predicate.has("effects") && predicate.get("effects").isJsonObject()) {
      for (var entry : predicate.getAsJsonObject("effects").entrySet()) {
        parts.add(readEffectEntry(entry.getKey(), entry.getValue()));
      }
    }
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("entity_properties predicate has no supported fields");
    }
    return wrapEntityTarget(target, parts.size() == 1
        ? parts.getFirst()
        : Conditions.allOf(parts.toArray(Condition[]::new)));
  }

  private Condition readEffectEntry(String effectId, JsonElement spec) {
    Key key = Key.key(effectId);
    if (spec == null || spec.isJsonNull() || !spec.isJsonObject() || spec.getAsJsonObject().isEmpty()) {
      return Conditions.potionPresent(key);
    }
    JsonObject obj = spec.getAsJsonObject();
    List<Condition> parts = new ArrayList<>();
    if (obj.has("amplifier")) {
      parts.add(boundedEffect(key, true, obj.get("amplifier")));
    }
    if (obj.has("duration")) {
      parts.add(boundedEffect(key, false, obj.get("duration")));
    }
    if (parts.isEmpty()) {
      return Conditions.potionPresent(key);
    }
    if (parts.size() == 1) {
      return parts.getFirst();
    }
    return Conditions.allOf(parts.toArray(Condition[]::new));
  }

  private Condition boundedEffect(Key key, boolean amplifier, JsonElement bound) {
    if (!bound.isJsonObject()) {
      return amplifier
          ? Conditions.potionAmplifier(key, RelationalOperator.EQUAL, bound.getAsInt())
          : Conditions.potionDuration(key, RelationalOperator.EQUAL, bound.getAsInt());
    }
    JsonObject bounds = bound.getAsJsonObject();
    if (bounds.has("min")) {
      return amplifier
          ? Conditions.potionAmplifier(
              key, RelationalOperator.GREATER_THAN_OR_EQUAL, bounds.get("min").getAsInt())
          : Conditions.potionDuration(
              key, RelationalOperator.GREATER_THAN_OR_EQUAL, bounds.get("min").getAsInt());
    }
    if (bounds.has("max")) {
      return amplifier
          ? Conditions.potionAmplifier(
              key, RelationalOperator.LESS_THAN_OR_EQUAL, bounds.get("max").getAsInt())
          : Conditions.potionDuration(
              key, RelationalOperator.LESS_THAN_OR_EQUAL, bounds.get("max").getAsInt());
    }
    return Conditions.potionPresent(key);
  }

  private Condition readWeather(JsonObject json) {
    if (!json.has("value") && !json.has("thundering") && !json.has("raining")) {
      return Conditions.always();
    }
    if (json.has("value")) {
      return Conditions.weather(WeatherState.valueOf(json.get("value").getAsString().toUpperCase(Locale.ROOT)));
    }
    boolean thundering = json.has("thundering") && json.get("thundering").getAsBoolean();
    boolean raining = json.has("raining") && json.get("raining").getAsBoolean();
    if (thundering) {
      return Conditions.weather(WeatherState.THUNDERING);
    }
    if (raining) {
      return Conditions.weather(WeatherState.RAINING);
    }
    return Conditions.weather(WeatherState.CLEAR);
  }

  private Condition readLocationCheck(JsonObject json) {
    JsonObject predicate = json.has("predicate") ? json.getAsJsonObject("predicate") : json;
    List<Condition> parts = new ArrayList<>();
    if (predicate.has("fluid")) {
      JsonElement fluid = predicate.get("fluid");
      List<String> fluids = fluid.isJsonObject() && fluid.getAsJsonObject().has("fluids")
          ? stringList(fluid.getAsJsonObject().get("fluids"))
          : stringList(fluid);
      Condition anyFluid = anyOfConditions(fluids.stream()
          .map(key -> Conditions.fluid(Key.key(key)))
          .toList());
      if (anyFluid != null) {
        parts.add(anyFluid);
      }
    }
    if (predicate.has("biomes")) {
      Condition anyBiome = anyOfConditions(stringList(predicate.get("biomes")).stream()
          .map(biome -> Conditions.biome(Key.key(biome)))
          .toList());
      if (anyBiome != null) {
        parts.add(anyBiome);
      }
    }
    if (predicate.has("position") && predicate.get("position").isJsonObject()) {
      parts.add(readPositionCondition(predicate.getAsJsonObject("position")));
    }
    if (predicate.has("dimension")) {
      parts.add(Conditions.dimension(Key.key(predicate.get("dimension").getAsString())));
    }
    if (predicate.has("light")) {
      parts.add(readLightCondition(predicate.get("light")));
    }
    if (predicate.has("can_see_sky")) {
      parts.add(Conditions.canSeeSky(predicate.get("can_see_sky").getAsBoolean()));
    }
    if (predicate.has("block") && predicate.get("block").isJsonObject()) {
      parts.add(readLocationBlock(predicate.getAsJsonObject("block")));
    }
    if (parts.isEmpty()) {
      throw new IllegalArgumentException(
          "location_check requires fluid, biomes, position, dimension, light, can_see_sky, or block");
    }
    Condition result = parts.size() == 1
        ? parts.getFirst()
        : Conditions.allOf(parts.toArray(Condition[]::new));
    if (json.has("offsetX") || json.has("offsetY") || json.has("offsetZ")) {
      int offsetX = json.has("offsetX") ? json.get("offsetX").getAsInt() : 0;
      int offsetY = json.has("offsetY") ? json.get("offsetY").getAsInt() : 0;
      int offsetZ = json.has("offsetZ") ? json.get("offsetZ").getAsInt() : 0;
      return Conditions.locationOffset(offsetX, offsetY, offsetZ, result);
    }
    return result;
  }

  private Condition readLocationBlock(JsonObject block) {
    List<Condition> parts = new ArrayList<>();
    if (block.has("blocks")) {
      Condition blocks = anyOfConditions(stringList(block.get("blocks")).stream()
          .map(id -> Conditions.blockId(Key.key(id)))
          .toList());
      if (blocks != null) {
        parts.add(blocks);
      }
    } else if (block.has("block")) {
      parts.add(Conditions.blockId(Key.key(block.get("block").getAsString())));
    }
    if (block.has("state") && block.get("state").isJsonObject()) {
      for (var entry : block.getAsJsonObject("state").entrySet()) {
        parts.add(readBlockPropertyEntry(entry.getKey(), entry.getValue()));
      }
    }
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("location_check block requires blocks or state");
    }
    if (parts.size() == 1) {
      return parts.getFirst();
    }
    return Conditions.allOf(parts.toArray(Condition[]::new));
  }

  private Condition readBlockStateProperty(JsonObject json) {
    List<Condition> parts = new ArrayList<>();
    if (json.has("block")) {
      parts.add(Conditions.blockId(Key.key(json.get("block").getAsString())));
    }
    if (json.has("properties") && json.get("properties").isJsonObject()) {
      for (var entry : json.getAsJsonObject("properties").entrySet()) {
        parts.add(readBlockPropertyEntry(entry.getKey(), entry.getValue()));
      }
    }
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("block_state_property requires block or properties");
    }
    if (parts.size() == 1) {
      return parts.getFirst();
    }
    return Conditions.allOf(parts.toArray(Condition[]::new));
  }

  private Condition readPlayerResource(JsonObject json) {
    String resource = json.has("resource")
        ? json.get("resource").getAsString()
        : requiredString(json, "resourceType");
    String operator = json.has("operator") ? json.get("operator").getAsString() : "==";
    double value = json.get("value").getAsDouble();
    return Conditions.playerResource(parseResource(resource), parseOperator(operator), value);
  }

  private Condition readJob(JsonObject json) {
    if (json.has("jobs") && json.get("jobs").isJsonArray()) {
      List<String> keys = new ArrayList<>();
      for (JsonElement el : json.getAsJsonArray("jobs")) {
        keys.add(el.getAsString());
      }
      return Conditions.jobAny(keys.toArray(String[]::new));
    }
    if (json.has("value")) {
      return Conditions.job(json.get("value").getAsString());
    }
    if (json.has("values") && json.get("values").isJsonArray()) {
      List<String> keys = new ArrayList<>();
      for (JsonElement el : json.getAsJsonArray("values")) {
        keys.add(el.getAsString());
      }
      return Conditions.jobAny(keys.toArray(String[]::new));
    }
    throw new IllegalArgumentException("job condition requires jobs, value, or values");
  }

  private Condition readLegacyPotion(JsonObject json) {
    String effect = requiredString(json, "effect");
    Key key = Key.key(effect.contains(":") ? effect : "minecraft:" + effect);
    if (json.has("amplifier") && json.has("operator")) {
      return Conditions.potionAmplifier(
          key, parseOperator(json.get("operator").getAsString()), json.get("amplifier").getAsInt());
    }
    return Conditions.potionPresent(key);
  }

  private Condition readTimeCheck(JsonObject json) {
    Long period = json.has("period") ? json.get("period").getAsLong() : null;
    JsonElement valueElement = json.get("value");
    Long min;
    Long max;
    if (valueElement.isJsonObject()) {
      JsonObject value = valueElement.getAsJsonObject();
      min = value.has("min") ? value.get("min").getAsLong() : null;
      max = value.has("max") ? value.get("max").getAsLong() : null;
    } else {
      long exact = valueElement.getAsLong();
      min = exact;
      max = exact;
    }
    return Conditions.timeCheck(min, max, period);
  }

  private Condition readEntityScores(JsonObject json) {
    EntityTarget target = readEntityTarget(json);
    if (!json.has("scores") || !json.get("scores").isJsonObject()) {
      throw new IllegalArgumentException("entity_scores requires scores");
    }
    Map<String, EntityScoresCondition.Bound> scores = new LinkedHashMap<>();
    for (var entry : json.getAsJsonObject("scores").entrySet()) {
      scores.put(entry.getKey(), readScoreBound(entry.getValue()));
    }
    return wrapEntityTarget(target, Conditions.entityScores(scores));
  }

  private static EntityTarget readEntityTarget(JsonObject json) {
    if (!json.has("entity")) {
      return EntityTarget.THIS;
    }
    return parseEntityTarget(json.get("entity").getAsString());
  }

  private static EntityTarget parseEntityTarget(String raw) {
    for (EntityTarget target : EntityTarget.values()) {
      if (target.jsonName().equals(raw)) {
        return target;
      }
    }
    throw new IllegalArgumentException("Unknown entity target: " + raw);
  }

  private static Condition wrapEntityTarget(EntityTarget target, Condition inner) {
    if (target == EntityTarget.THIS) {
      return inner;
    }
    return Conditions.targeted(target, inner);
  }

  private Condition readKilledByPlayer(JsonObject json) {
    boolean expected = json.has("value") ? json.get("value").getAsBoolean() : true;
    return Conditions.killedByPlayer(expected);
  }

  private static EntityScoresCondition.Bound readScoreBound(JsonElement element) {
    if (element.isJsonPrimitive()) {
      int exact = element.getAsInt();
      return new EntityScoresCondition.Bound(exact, exact);
    }
    JsonObject bounds = element.getAsJsonObject();
    Integer min = bounds.has("min") ? bounds.get("min").getAsInt() : null;
    Integer max = bounds.has("max") ? bounds.get("max").getAsInt() : null;
    return new EntityScoresCondition.Bound(min, max);
  }

  private static Condition readPositionCondition(JsonObject position) {
    Double minX = null;
    Double maxX = null;
    Double minY = null;
    Double maxY = null;
    Double minZ = null;
    Double maxZ = null;
    if (position.has("x")) {
      Double[] x = readAxisBounds(position.get("x"));
      minX = x[0];
      maxX = x[1];
    }
    if (position.has("y")) {
      Double[] y = readAxisBounds(position.get("y"));
      minY = y[0];
      maxY = y[1];
    }
    if (position.has("z")) {
      Double[] z = readAxisBounds(position.get("z"));
      minZ = z[0];
      maxZ = z[1];
    }
    return Conditions.position(minX, maxX, minY, maxY, minZ, maxZ);
  }

  private static Condition readLightCondition(JsonElement lightElement) {
    if (!lightElement.isJsonObject()) {
      throw new IllegalArgumentException("location_check light must be an object");
    }
    JsonObject light = lightElement.getAsJsonObject();
    Integer minLevel = null;
    Integer maxLevel = null;
    if (light.has("light")) {
      Integer[] bounds = readIntegerBounds(light.get("light"));
      minLevel = bounds[0];
      maxLevel = bounds[1];
    }
    return Conditions.light(minLevel, maxLevel, null, null, null, null);
  }

  private static Double[] readAxisBounds(JsonElement element) {
    if (element.isJsonPrimitive()) {
      double exact = element.getAsDouble();
      return new Double[] {exact, exact};
    }
    JsonObject bounds = element.getAsJsonObject();
    Double min = bounds.has("min") ? bounds.get("min").getAsDouble() : null;
    Double max = bounds.has("max") ? bounds.get("max").getAsDouble() : null;
    return new Double[] {min, max};
  }

  private static Integer[] readIntegerBounds(JsonElement element) {
    if (element.isJsonPrimitive()) {
      int exact = element.getAsInt();
      return new Integer[] {exact, exact};
    }
    JsonObject bounds = element.getAsJsonObject();
    Integer min = bounds.has("min") ? bounds.get("min").getAsInt() : null;
    Integer max = bounds.has("max") ? bounds.get("max").getAsInt() : null;
    return new Integer[] {min, max};
  }

  private List<Condition> readTerms(JsonObject json, String field) {
    if (!json.has(field) || !json.get(field).isJsonArray()) {
      return List.of();
    }
    List<Condition> terms = new ArrayList<>();
    for (JsonElement el : json.getAsJsonArray(field)) {
      if (!el.isJsonObject()) {
        throw new IllegalArgumentException(field + " entries must be objects");
      }
      terms.add(readObject(el.getAsJsonObject()));
    }
    return terms;
  }

  private static void addFlag(
      List<Condition> parts,
      JsonObject flags,
      String name,
      java.util.function.Function<Boolean, Condition> factory) {
    if (flags.has(name)) {
      parts.add(factory.apply(flags.get(name).getAsBoolean()));
    }
  }

  private static JsonObject entityTypeJson(String type) {
    JsonObject predicate = new JsonObject();
    predicate.addProperty("type", type);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject gameModeJson(String gameMode) {
    JsonObject specific = new JsonObject();
    specific.addProperty("type", "player");
    com.google.gson.JsonArray modes = new com.google.gson.JsonArray();
    modes.add(gameMode);
    specific.add("gamemode", modes);
    JsonObject predicate = new JsonObject();
    predicate.add("type_specific", specific);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject blockStateJson(
      String blockId, String propertyName, String propertyValue) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:block_state_property");
    if (blockId != null) {
      root.addProperty("block", blockId);
    }
    if (propertyName != null) {
      JsonObject properties = new JsonObject();
      properties.addProperty(propertyName, propertyValue);
      root.add("properties", properties);
    }
    return root;
  }

  private static JsonObject entityFlags(JsonObject flags) {
    JsonObject predicate = new JsonObject();
    predicate.add("flags", flags);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject flagObject(String name, boolean value) {
    JsonObject flags = new JsonObject();
    flags.addProperty(name, value);
    return flags;
  }

  private static JsonObject entityLocationBiome(String biome) {
    JsonObject location = new JsonObject();
    JsonArray biomes = new JsonArray();
    biomes.add(biome);
    location.add("biomes", biomes);
    JsonObject predicate = new JsonObject();
    predicate.add("location", location);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  /**
   * Strict operators cannot be expressed by vanilla's inclusive min/max
   * bounds, so they are encoded losslessly as inversions: {@code >X} as
   * {@code not(<=X)}, {@code <X} as {@code not(>=X)}, {@code !=X} as
   * {@code not(=X)}.
   */
  private static JsonElement effectJson(
      String effect, String field, RelationalOperator operator, int expected) {
    return switch (operator) {
      case GREATER_THAN -> invertedJson(
          effectJson(effect, field, RelationalOperator.LESS_THAN_OR_EQUAL, expected));
      case LESS_THAN -> invertedJson(
          effectJson(effect, field, RelationalOperator.GREATER_THAN_OR_EQUAL, expected));
      case NOT_EQUAL -> invertedJson(effectJson(effect, field, RelationalOperator.EQUAL, expected));
      case GREATER_THAN_OR_EQUAL -> effectRoot(effect, boundSpec(field, "min", expected));
      case LESS_THAN_OR_EQUAL -> effectRoot(effect, boundSpec(field, "max", expected));
      case EQUAL -> effectRoot(effect, plainSpec(field, expected));
    };
  }

  private static JsonObject boundSpec(String field, String bound, int expected) {
    JsonObject bounds = new JsonObject();
    bounds.addProperty(bound, expected);
    JsonObject spec = new JsonObject();
    spec.add(field, bounds);
    return spec;
  }

  private static JsonObject plainSpec(String field, int expected) {
    JsonObject spec = new JsonObject();
    spec.addProperty(field, expected);
    return spec;
  }

  private static JsonObject effectRoot(String effect, JsonObject spec) {
    JsonObject effects = new JsonObject();
    effects.add(effect, spec);
    JsonObject predicate = new JsonObject();
    predicate.add("effects", effects);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject locationFluid(String fluid) {
    JsonObject fluidObj = new JsonObject();
    fluidObj.addProperty("fluids", fluid);
    JsonObject predicate = new JsonObject();
    predicate.add("fluid", fluidObj);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:location_check");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject timeCheckJson(TimeCheckCondition time) {
    JsonObject value = new JsonObject();
    if (time.min() != null) {
      value.addProperty("min", time.min());
    }
    if (time.max() != null) {
      value.addProperty("max", time.max());
    }
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:time_check");
    root.add("value", value);
    if (time.period() != null) {
      root.addProperty("period", time.period());
    }
    return root;
  }

  private static JsonObject entityScoresJson(EntityScoresCondition scores) {
    JsonObject scoreObject = new JsonObject();
    for (Map.Entry<String, EntityScoresCondition.Bound> entry : scores.scores().entrySet()) {
      scoreObject.add(entry.getKey(), writeScoreBound(entry.getValue()));
    }
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_scores");
    root.addProperty("entity", "this");
    root.add("scores", scoreObject);
    return root;
  }

  private static JsonElement writeScoreBound(EntityScoresCondition.Bound bound) {
    if (bound.min() != null && bound.max() != null && bound.min().equals(bound.max())) {
      return new com.google.gson.JsonPrimitive(bound.min());
    }
    JsonObject bounds = new JsonObject();
    if (bound.min() != null) {
      bounds.addProperty("min", bound.min());
    }
    if (bound.max() != null) {
      bounds.addProperty("max", bound.max());
    }
    return bounds;
  }
  private JsonObject locationOffsetJson(LocationOffsetCondition offset) {
    JsonObject predicate;
    if (isBlockPredicateCondition(offset.condition())) {
      predicate = new JsonObject();
      predicate.add("block", writeBlockPredicate(offset.condition()));
    } else {
      JsonObject inner = writeElement(offset.condition()).getAsJsonObject();
      String conditionId = inner.has("condition") ? inner.get("condition").getAsString() : "";
      if (!"minecraft:location_check".equals(conditionId)
          || !inner.has("predicate")
          || !inner.get("predicate").isJsonObject()) {
        throw new IllegalArgumentException(
            "LocationOffset inner must be a block or location condition");
      }
      predicate = inner.getAsJsonObject("predicate");
    }
    JsonObject root = locationCheckJson(predicate);
    root.addProperty("offsetX", offset.offsetX());
    root.addProperty("offsetY", offset.offsetY());
    root.addProperty("offsetZ", offset.offsetZ());
    return root;
  }

  private static boolean isBlockPredicateCondition(Condition condition) {
    return condition instanceof BlockIdCondition
        || condition instanceof BlockPropertyCondition
        || condition instanceof BlockPropertyRangeCondition
        || (condition instanceof AllOfCondition all
            && !all.terms().isEmpty()
            && all.terms().stream().allMatch(GsonConditionSerializer::isBlockPredicateCondition));
  }


  private static JsonObject killedByPlayerJson(KilledByPlayerCondition killed) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:killed_by_player");
    root.addProperty("value", killed.expected());
    return root;
  }

  private static JsonObject positionLocationCheckJson(PositionCondition position) {
    JsonObject positionObject = new JsonObject();
    JsonObject x = axisBoundsObject(position.minX(), position.maxX());
    if (x != null) {
      positionObject.add("x", x);
    }
    JsonObject y = axisBoundsObject(position.minY(), position.maxY());
    if (y != null) {
      positionObject.add("y", y);
    }
    JsonObject z = axisBoundsObject(position.minZ(), position.maxZ());
    if (z != null) {
      positionObject.add("z", z);
    }
    JsonObject predicate = new JsonObject();
    predicate.add("position", positionObject);
    return locationCheckJson(predicate);
  }

  private static JsonObject dimensionLocationCheckJson(DimensionCondition dimension) {
    JsonObject predicate = new JsonObject();
    predicate.addProperty("dimension", dimension.dimensionKey().asString());
    return locationCheckJson(predicate);
  }

  private static JsonObject lightLocationCheckJson(LightCondition light) {
    if (light.minSky() != null || light.maxSky() != null
        || light.minBlock() != null || light.maxBlock() != null) {
      throw new IllegalArgumentException(
          "LightCondition sky/block bounds cannot be expressed in vanilla JE 26.2 "
              + "location JSON; only the combined light bound is supported");
    }
    JsonObject lightObject = new JsonObject();
    JsonElement levelBounds = intBoundsElement(light.minLevel(), light.maxLevel());
    if (levelBounds != null) {
      lightObject.add("light", levelBounds);
    }
    JsonObject predicate = new JsonObject();
    predicate.add("light", lightObject);
    return locationCheckJson(predicate);
  }

  private static JsonObject canSeeSkyLocationCheckJson(CanSeeSkyCondition canSeeSky) {
    JsonObject predicate = new JsonObject();
    predicate.addProperty("can_see_sky", canSeeSky.expected());
    return locationCheckJson(predicate);
  }

  private static JsonObject locationCheckJson(JsonObject predicate) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:location_check");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject axisBoundsObject(Double min, Double max) {
    if (min == null && max == null) {
      return null;
    }
    JsonObject bounds = new JsonObject();
    if (min != null) {
      bounds.addProperty("min", min);
    }
    if (max != null) {
      bounds.addProperty("max", max);
    }
    return bounds;
  }

  private static JsonElement intBoundsElement(Integer min, Integer max) {
    if (min == null && max == null) {
      return null;
    }
    if (min != null && max != null && min.equals(max)) {
      return new com.google.gson.JsonPrimitive(min);
    }
    JsonObject bounds = new JsonObject();
    if (min != null) {
      bounds.addProperty("min", min);
    }
    if (max != null) {
      bounds.addProperty("max", max);
    }
    return bounds;
  }


  private static JsonObject teamJson(TeamCondition team) {
    JsonObject predicate = new JsonObject();
    predicate.addProperty("team", team.expected());
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static Condition readPing(JsonObject json) {
    String op = json.has("operator") ? json.get("operator").getAsString() : "<=";
    int ms = json.has("milliseconds") ? json.get("milliseconds").getAsInt() : 0;
    return Conditions.ping(parseOperator(op), ms);
  }

  private static JsonObject pingJson(PingCondition ping) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:ping");
    root.addProperty("operator", operatorName(ping.operator()));
    root.addProperty("milliseconds", ping.milliseconds());
    return root;
  }


  private static Condition readEmptySlots(JsonObject json) {
    int minimum = json.get("minimum").getAsInt();
    boolean includeOffhand = json.has("include_offhand") && json.get("include_offhand").getAsBoolean();
    boolean includeHotbar = true;
    if (json.has("include_hotbar")) {
      includeHotbar = json.get("include_hotbar").getAsBoolean();
    }
    return Conditions.emptySlots(minimum, includeOffhand, includeHotbar);
  }

  private static JsonObject emptySlotsJson(EmptySlotsCondition empty) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:empty_slots");
    root.addProperty("minimum", empty.minimum());
    root.addProperty("include_offhand", empty.includeOffhand());
    root.addProperty("include_hotbar", empty.includeHotbar());
    return root;
  }

  private static Condition readSheep(JsonObject json) {
    Boolean sheared = json.has("sheared") && json.get("sheared").isJsonPrimitive()
        ? json.get("sheared").getAsBoolean() : null;
    Key color = json.has("color") && json.get("color").isJsonPrimitive()
        ? Key.key(json.get("color").getAsString()) : null;
    return Conditions.sheep(sheared, color);
  }

  private static Condition readFrog(JsonObject json) {
    return Conditions.frog(Key.key(json.get("variant").getAsString()));
  }

  private static JsonObject sheepJson(SheepCondition sheep) {
    JsonObject sub = new JsonObject();
    if (sheep.sheared() != null) {
      sub.addProperty("sheared", sheep.sheared());
    }
    if (sheep.color() != null) {
      sub.addProperty("color", sheep.color().asString());
    }
    JsonObject specific = new JsonObject();
    specific.add("sheep", sub);
    JsonObject predicate = new JsonObject();
    predicate.add("type_specific", specific);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject frogJson(FrogCondition frog) {
    JsonObject sub = new JsonObject();
    sub.addProperty("variant", frog.variant().asString());
    JsonObject specific = new JsonObject();
    specific.add("frog", sub);
    JsonObject predicate = new JsonObject();
    predicate.add("type_specific", specific);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }


  private static Condition readCat(JsonObject json) {
    return Conditions.cat(Key.key(json.get("variant").getAsString()));
  }

  private static Condition readWolf(JsonObject json) {
    Key collarColor = json.has("collar_color")
        ? Key.key(json.get("collar_color").getAsString()) : null;
    Key variant = json.has("variant") ? Key.key(json.get("variant").getAsString()) : null;
    return Conditions.wolf(collarColor, variant);
  }

  private static Condition readTropicalFish(JsonObject json) {
    Integer variant = json.has("variant") && json.get("variant").isJsonPrimitive()
        ? json.get("variant").getAsInt() : null;
    Key pattern = json.has("pattern") ? Key.key(json.get("pattern").getAsString()) : null;
    Key baseColor = json.has("base_color") ? Key.key(json.get("base_color").getAsString()) : null;
    Key patternColor = json.has("pattern_color")
        ? Key.key(json.get("pattern_color").getAsString()) : null;
    return Conditions.tropicalFish(variant, pattern, baseColor, patternColor);
  }

  private static Condition readVillager(JsonObject json) {
    Key type = json.has("type") ? Key.key(json.get("type").getAsString()) : null;
    Key profession = json.has("profession") ? Key.key(json.get("profession").getAsString()) : null;
    Integer minimumLevel = null;
    Integer maximumLevel = null;
    if (json.has("level")) {
      JsonElement level = json.get("level");
      if (level.isJsonPrimitive()) {
        minimumLevel = level.getAsInt();
        maximumLevel = level.getAsInt();
      } else if (level.isJsonObject()) {
        JsonObject bounds = level.getAsJsonObject();
        minimumLevel = bounds.has("min") ? bounds.get("min").getAsInt() : null;
        maximumLevel = bounds.has("max") ? bounds.get("max").getAsInt() : null;
      }
    }
    return Conditions.villager(type, profession, minimumLevel, maximumLevel);
  }

  private static Condition readHorse(JsonObject json) {
    Key color = json.has("variant") ? Key.key(json.get("variant").getAsString()) : null;
    Set<Key> armorItems = json.has("armor")
        ? readArmorItems(json.getAsJsonObject("armor"))
        : Set.of();
    return Conditions.horse(color, armorItems);
  }

  private static Set<Key> readArmorItems(JsonObject armor) {
    if (armor.has("items")) {
      return stringList(armor.get("items")).stream()
          .map(Key::key)
          .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    if (armor.has("item")) {
      return Set.of(Key.key(armor.get("item").getAsString()));
    }
    throw new IllegalArgumentException("horse armor predicate requires items");
  }

  private static JsonObject wolfJson(WolfVariantCondition wolf) {
    JsonObject sub = new JsonObject();
    if (wolf.collarColor() != null) {
      sub.addProperty("collar_color", wolf.collarColor().asString());
    }
    if (wolf.variant() != null) {
      sub.addProperty("variant", wolf.variant().asString());
    }
    return sub;
  }

  private static JsonObject tropicalFishJson(TropicalFishCondition fish) {
    JsonObject sub = new JsonObject();
    if (fish.variant() != null) {
      sub.addProperty("variant", fish.variant());
    }
    if (fish.pattern() != null) {
      sub.addProperty("pattern", fish.pattern().asString());
    }
    if (fish.baseColor() != null) {
      sub.addProperty("base_color", fish.baseColor().asString());
    }
    if (fish.patternColor() != null) {
      sub.addProperty("pattern_color", fish.patternColor().asString());
    }
    return sub;
  }

  private static JsonObject villagerJson(VillagerCondition villager) {
    JsonObject sub = new JsonObject();
    if (villager.type() != null) {
      sub.addProperty("type", villager.type().asString());
    }
    if (villager.profession() != null) {
      sub.addProperty("profession", villager.profession().asString());
    }
    if (villager.minimumLevel() != null || villager.maximumLevel() != null) {
      JsonObject level = new JsonObject();
      if (villager.minimumLevel() != null) {
        level.addProperty("min", villager.minimumLevel());
      }
      if (villager.maximumLevel() != null) {
        level.addProperty("max", villager.maximumLevel());
      }
      sub.add("level", level);
    }
    return sub;
  }

  private static JsonObject horseJson(HorseCondition horse) {
    JsonObject sub = new JsonObject();
    if (horse.color() != null) {
      sub.addProperty("variant", horse.color().asString());
    }
    if (horse.armorItems().isEmpty() == false) {
      JsonArray items = new JsonArray();
      for (Key item : horse.armorItems()) {
        items.add(item.asString());
      }
      JsonObject armor = new JsonObject();
      armor.add("items", items);
      sub.add("armor", armor);
    }
    return sub;
  }

  private static JsonObject typeSpecificJson(String key, JsonObject sub) {
    JsonObject specific = new JsonObject();
    specific.add(key, sub);
    JsonObject predicate = new JsonObject();
    predicate.add("type_specific", specific);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject catJson(CatCondition cat) {
    JsonObject sub = new JsonObject();
    sub.addProperty("variant", cat.variant().asString());
    JsonObject specific = new JsonObject();
    specific.add("cat", sub);
    JsonObject predicate = new JsonObject();
    predicate.add("type_specific", specific);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static Condition readRequiredItemCount(JsonObject json) {
    String raw = json.get("material").getAsString();
    Key material = raw.contains(":") ? Key.key(raw) : Key.key("minecraft:" + raw);
    int minimum = json.get("minimum").getAsInt();
    return Conditions.requiredItemCount(material, minimum);
  }

  private static JsonObject requiredItemCountJson(RequiredItemCountCondition required) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:required_item_count");
    root.addProperty("material", required.material().asString());
    root.addProperty("minimum", required.minimum());
    return root;
  }


  private static Condition readPlaytime(JsonObject json) {
    String op = json.has("operator") ? json.get("operator").getAsString() : "==";
    long ticks = json.get("ticks").getAsLong();
    return Conditions.playtime(parseOperator(op), ticks);
  }

  private static JsonObject playtimeJson(PlaytimeCondition playtime) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:playtime");
    root.addProperty("operator", operatorName(playtime.operator()));
    root.addProperty("ticks", playtime.ticks());
    return root;
  }

  private static Condition readArmorSet(JsonObject json) {
    List<String> rawItems = stringList(json.get("items"));
    Set<Key> items = rawItems.stream().map(Key::key).collect(java.util.stream.Collectors.toUnmodifiableSet());
    return Conditions.armorSet(items);
  }

  private static JsonObject armorSetJson(ArmorSetCondition armor) {
    JsonArray array = new JsonArray();
    for (Key item : armor.items()) {
      array.add(item.asString());
    }
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:armor_set");
    root.add("items", array);
    return root;
  }


  private static Condition readInvulnerableFrames(JsonObject json) {
    String op = json.has("operator") ? json.get("operator").getAsString() : "==";
    int ticks = json.get("ticks").getAsInt();
    return Conditions.invulnerableFrames(parseOperator(op), ticks);
  }

  private static JsonObject invulnerableFramesJson(InvulnerableFramesCondition inv) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:invulnerable_frames");
    root.addProperty("operator", operatorName(inv.operator()));
    root.addProperty("ticks", inv.ticks());
    return root;
  }

  private static Condition readAdvancement(JsonObject json) {
    return Conditions.advancement(Key.key(json.get("advancement").getAsString()));
  }

  private static JsonObject advancementJson(AdvancementCondition adv) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:advancement");
    root.addProperty("advancement", adv.advancement().asString());
    return root;
  }


  private JsonObject vehicleJson(VehicleCondition vehicle) {
    JsonObject predicate = new JsonObject();
    JsonElement inner = writeElement(vehicle.condition());
    if (inner.isJsonObject() && inner.getAsJsonObject().has("predicate")) {
      predicate.add("vehicle", inner.getAsJsonObject().get("predicate"));
    } else {
      predicate.add("vehicle", inner);
    }
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private JsonObject passengerJson(PassengerCondition passenger) {
    JsonObject predicate = new JsonObject();
    JsonElement inner = writeElement(passenger.condition());
    if (inner.isJsonObject() && inner.getAsJsonObject().has("predicate")) {
      predicate.add("passenger", inner.getAsJsonObject().get("predicate"));
    } else {
      predicate.add("passenger", inner);
    }
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }


  private static Condition readBlockPropertyEntry(String name, JsonElement value) {
    if (value.isJsonObject()) {
      JsonObject obj = value.getAsJsonObject();
      Integer min = obj.has("min") ? obj.get("min").getAsInt() : null;
      Integer max = obj.has("max") ? obj.get("max").getAsInt() : null;
      if (min != null || max != null) {
        int lo = min == null ? Integer.MIN_VALUE : min;
        int hi = max == null ? Integer.MAX_VALUE : max;
        return Conditions.blockPropertyRange(name, lo, hi);
      }
    }
    return Conditions.blockProperty(name, firstString(value));
  }

  private static Condition readBlockStateRange(JsonObject json) {
    String name = requiredString(json, "name");
    int min = json.has("min") ? json.get("min").getAsInt() : Integer.MIN_VALUE;
    int max = json.has("max") ? json.get("max").getAsInt() : Integer.MAX_VALUE;
    return Conditions.blockPropertyRange(name, min, max);
  }

  private static JsonObject blockPropertyRangeJson(BlockPropertyRangeCondition range) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:block_state_range");
    root.addProperty("name", range.name());
    if (range.min() != Integer.MIN_VALUE) {
      root.addProperty("min", range.min());
    }
    if (range.max() != Integer.MAX_VALUE) {
      root.addProperty("max", range.max());
    }
    return root;
  }

  private static Condition readPeriodicTick(JsonObject json) {
    int period = json.get("period").getAsInt();
    int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
    return Conditions.periodicTick(period, offset);
  }


  private static Condition readRandomChance(JsonObject json) {
    double chance = json.get("chance").getAsDouble();
    return Conditions.randomChance(chance);
  }

  private static JsonObject randomChanceJson(RandomChanceCondition chance) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:random_chance");
    root.addProperty("chance", chance.chance());
    return root;
  }

  private static JsonObject periodicTickJson(PeriodicTickCondition tick) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:periodic_tick");
    root.addProperty("period", tick.period());
    root.addProperty("offset", tick.offset());
    return root;
  }

  private static Condition readOffhandItem(JsonObject json) {
    Set<Key> materials = json.has("material")
        ? stringList(json.get("material")).stream().map(Key::key).collect(java.util.stream.Collectors.toUnmodifiableSet())
        : Set.of();
    int minimumAmount = json.has("minimum_amount") ? json.get("minimum_amount").getAsInt() : 0;
    Integer customModelData = json.has("custom_model_data")
        ? json.get("custom_model_data").getAsInt()
        : null;
    return Conditions.offhandItem(materials, minimumAmount, customModelData);
  }

  private static JsonObject offhandItemJson(OffhandItemCondition offhand) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:inventory_offhand_item");
    if (!offhand.materials().isEmpty()) {
      JsonArray materials = new JsonArray();
      for (Key material : offhand.materials()) {
        materials.add(material.asString());
      }
      root.add("material", materials);
    }
    if (offhand.minimumAmount() != 0) {
      root.addProperty("minimum_amount", offhand.minimumAmount());
    }
    if (offhand.customModelData() != null) {
      root.addProperty("custom_model_data", offhand.customModelData());
    }
    return root;
  }

  private static Condition readActiveCooldown(JsonObject json) {
    Key key = Key.key(requiredString(json, "key").contains(":")
        ? requiredString(json, "key")
        : "minecraft:" + requiredString(json, "key"));
    String source = json.has("source") ? json.get("source").getAsString() : null;
    Boolean active = json.has("active") ? json.get("active").getAsBoolean() : null;
    Integer minimumTicks = json.has("minimum_remaining_ticks")
        ? json.get("minimum_remaining_ticks").getAsInt()
        : null;
    return Conditions.activeCooldown(key, source, active, minimumTicks);
  }

  private static JsonObject activeCooldownJson(ActiveCooldownCondition cooldown) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:active_cooldown");
    root.addProperty("key", cooldown.key().asString());
    if (cooldown.source() != null) {
      root.addProperty("source", cooldown.source());
    }
    if (cooldown.active() != null) {
      root.addProperty("active", cooldown.active());
    }
    if (cooldown.minimumRemainingTicks() != null) {
      root.addProperty("minimum_remaining_ticks", cooldown.minimumRemainingTicks());
    }
    return root;
  }

  private static Condition readItemTypeCount(JsonObject json) {
    if (!json.has("items") || !json.get("items").isJsonArray()) {
      throw new IllegalArgumentException("item_type_count requires items");
    }
    Set<Key> items = stringList(json.get("items")).stream()
        .map(Key::key)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    JsonObject count = json.has("count") && json.get("count").isJsonObject()
        ? json.getAsJsonObject("count")
        : new JsonObject();
    Integer minimum = count.has("min") ? count.get("min").getAsInt() : null;
    Integer maximum = count.has("max") ? count.get("max").getAsInt() : null;
    return Conditions.itemTypeCount(items, minimum, maximum);
  }

  private static JsonObject itemTypeCountJson(ItemTypeCountCondition count) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:item_type_count");
    JsonArray items = new JsonArray();
    for (Key item : count.items()) {
      items.add(item.asString());
    }
    root.add("items", items);
    if (count.minimum() != null || count.maximum() != null) {
      JsonObject bounds = new JsonObject();
      if (count.minimum() != null) {
        bounds.addProperty("min", count.minimum());
      }
      if (count.maximum() != null) {
        bounds.addProperty("max", count.maximum());
      }
      root.add("count", bounds);
    }
    return root;
  }

  private static Condition readItemDurability(JsonObject json) {
    JsonObject bounds = json.has("durability") && json.get("durability").isJsonObject()
        ? json.getAsJsonObject("durability")
        : json;
    Integer minimum = bounds.has("min") ? bounds.get("min").getAsInt() : null;
    Integer maximum = bounds.has("max") ? bounds.get("max").getAsInt() : null;
    return Conditions.itemDurability(minimum, maximum);
  }

  private static JsonObject itemDurabilityJson(ItemDurabilityCondition durability) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:item_durability");
    JsonObject bounds = new JsonObject();
    if (durability.minimum() != null) {
      bounds.addProperty("min", durability.minimum());
    }
    if (durability.maximum() != null) {
      bounds.addProperty("max", durability.maximum());
    }
    root.add("durability", bounds);
    return root;
  }

  private static Condition readItemEnchantments(JsonObject json) {
    if (!json.has("enchantments") || !json.get("enchantments").isJsonArray()) {
      throw new IllegalArgumentException("item_enchantments requires enchantments array");
    }
    Map<Key, ItemEnchantmentsCondition.LevelBound> bounds = new LinkedHashMap<>();
    for (JsonElement element : json.getAsJsonArray("enchantments")) {
      if (element.isJsonNull() || !element.isJsonObject()) {
        throw new IllegalArgumentException("enchantment entries must be objects");
      }
      JsonObject entry = element.getAsJsonObject();
      Key key = Key.key(requiredString(entry, "enchantment"));
      JsonObject levels = entry.has("levels") && entry.get("levels").isJsonObject()
          ? entry.getAsJsonObject("levels")
          : new JsonObject();
      Integer minimum = levels.has("min") ? levels.get("min").getAsInt() : null;
      Integer maximum = levels.has("max") ? levels.get("max").getAsInt() : null;
      bounds.put(key, new ItemEnchantmentsCondition.LevelBound(minimum, maximum));
    }
    return Conditions.itemEnchantments(bounds);
  }

  private static JsonObject itemEnchantmentsJson(ItemEnchantmentsCondition enchantments) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:item_enchantments");
    JsonArray entries = new JsonArray();
    for (var entry : enchantments.enchantments().entrySet()) {
      JsonObject item = new JsonObject();
      item.addProperty("enchantment", entry.getKey().asString());
      ItemEnchantmentsCondition.LevelBound bound = entry.getValue();
      if (bound.minimum() != null || bound.maximum() != null) {
        JsonObject levels = new JsonObject();
        if (bound.minimum() != null) {
          levels.addProperty("min", bound.minimum());
        }
        if (bound.maximum() != null) {
          levels.addProperty("max", bound.maximum());
        }
        item.add("levels", levels);
      }
      entries.add(item);
    }
    root.add("enchantments", entries);
    return root;
  }

  private static Condition readItemTrim(JsonObject json) {
    JsonObject components = json.has("components") && json.get("components").isJsonObject()
        ? json.getAsJsonObject("components")
        : json;
    for (var entry : components.entrySet()) {
      if ("minecraft:trim".equals(entry.getKey()) == false) {
        throw new IllegalArgumentException(
            "item_trim does not support component: " + entry.getKey());
      }
    }
    if (!components.has("minecraft:trim") || !components.get("minecraft:trim").isJsonObject()) {
      throw new IllegalArgumentException("item_trim requires components.minecraft:trim");
    }
    JsonObject trim = components.getAsJsonObject("minecraft:trim");
    for (var entry : trim.entrySet()) {
      String field = entry.getKey();
      boolean supported = "material".equals(field) || "pattern".equals(field);
      if (supported == false) {
        throw new IllegalArgumentException(
            "item_trim minecraft:trim does not support field: " + field);
      }
    }
    Key material = trim.has("material") ? Key.key(trim.get("material").getAsString()) : null;
    Key pattern = trim.has("pattern") ? Key.key(trim.get("pattern").getAsString()) : null;
    return Conditions.itemTrim(material, pattern);
  }

  private static JsonObject itemTrimJson(ItemTrimCondition trim) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:item_trim");
    JsonObject trimObject = new JsonObject();
    if (trim.material() != null) {
      trimObject.addProperty("material", trim.material().asString());
    }
    if (trim.pattern() != null) {
      trimObject.addProperty("pattern", trim.pattern().asString());
    }
    JsonObject components = new JsonObject();
    components.add("minecraft:trim", trimObject);
    root.add("components", components);
    return root;
  }

  private Condition readSteppingOn(JsonObject json) {
    Condition inner = readSteppingOnInner(json);
    return Conditions.steppingOn(inner);
  }

  private Condition readSteppingOnInner(JsonObject json) {
    if (json.has("block") && json.get("block").isJsonObject()) {
      return readLocationBlock(json.getAsJsonObject("block"));
    }
    if (json.has("predicate") && json.get("predicate").isJsonObject()) {
      JsonObject predicate = json.getAsJsonObject("predicate");
      if (predicate.has("block") && predicate.get("block").isJsonObject()) {
        return readLocationBlock(predicate.getAsJsonObject("block"));
      }
    }
    throw new IllegalArgumentException("stepping_on requires block or predicate.block");
  }

  private static Condition readEquipmentSlot(EquipmentSlotKey slot, JsonElement spec) {
    if (spec == null || spec.isJsonNull() || !spec.isJsonObject()) {
      throw new IllegalArgumentException("equipment slot requires an item predicate object");
    }
    JsonObject matcher = spec.getAsJsonObject();
    Set<Key> items;
    if (matcher.has("items")) {
      items = stringList(matcher.get("items")).stream()
          .map(Key::key)
          .collect(java.util.stream.Collectors.toUnmodifiableSet());
    } else if (matcher.has("item")) {
      items = Set.of(Key.key(matcher.get("item").getAsString()));
    } else {
      throw new IllegalArgumentException("equipment slot predicate requires items");
    }
    if (matcher.has("components")) {
      throw new IllegalArgumentException(
          "equipment slot components are not supported; use modularjobs:item_trim "
              + "or another subject-scoped condition");
    }
    JsonObject count = matcher.has("count") && matcher.get("count").isJsonObject()
        ? matcher.getAsJsonObject("count")
        : new JsonObject();
    Integer minimum = count.has("min") ? count.get("min").getAsInt() : null;
    Integer maximum = count.has("max") ? count.get("max").getAsInt() : null;
    return Conditions.equipmentItem(slot, items, minimum, maximum);
  }

  private static JsonObject equipmentItemJson(EquipmentItemCondition equipment) {
    JsonObject slots = new JsonObject();
    slots.add(equipment.slot().jsonName(), equipmentSlotObject(equipment));
    JsonObject predicate = new JsonObject();
    predicate.add("equipment", slots);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject equipmentSlotObject(EquipmentItemCondition equipment) {
    JsonObject slotObject = new JsonObject();
    JsonArray items = new JsonArray();
    for (Key item : equipment.items()) {
      items.add(item.asString());
    }
    slotObject.add("items", items);
    if (equipment.minimum() != null || equipment.maximum() != null) {
      JsonObject count = new JsonObject();
      if (equipment.minimum() != null) {
        count.addProperty("min", equipment.minimum());
      }
      if (equipment.maximum() != null) {
        count.addProperty("max", equipment.maximum());
      }
      slotObject.add("count", count);
    }
    return slotObject;
  }

  private JsonObject steppingOnJson(SteppingOnCondition stepping) {
    JsonObject block = writeBlockPredicate(stepping.condition());
    JsonObject steppingOn = new JsonObject();
    steppingOn.add("block", block);
    JsonObject predicate = new JsonObject();
    predicate.add("stepping_on", steppingOn);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private Condition readMatchBlock(JsonObject json) {
    JsonObject predicate = json.has("predicate") && json.get("predicate").isJsonObject()
        ? json.getAsJsonObject("predicate")
        : json;
    return Conditions.matchBlock(readLocationBlock(predicate));
  }

  private static JsonObject matchBlockJson(MatchBlockCondition match) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:match_block");
    root.add("predicate", writeBlockPredicate(match.condition()));
    return root;
  }

  private static JsonObject writeBlockPredicate(Condition condition) {
    if (condition instanceof BlockIdCondition blockId) {
      JsonObject root = new JsonObject();
      JsonArray blocks = new JsonArray();
      blocks.add(blockId.blockId().asString());
      root.add("blocks", blocks);
      return root;
    }
    if (condition instanceof BlockPropertyCondition prop) {
      JsonObject root = new JsonObject();
      JsonObject state = new JsonObject();
      state.addProperty(prop.name(), prop.value());
      root.add("state", state);
      return root;
    }
    if (condition instanceof BlockPropertyRangeCondition range) {
      JsonObject root = new JsonObject();
      JsonObject state = new JsonObject();
      JsonObject bounds = new JsonObject();
      if (range.min() != Integer.MIN_VALUE) {
        bounds.addProperty("min", range.min());
      }
      if (range.max() != Integer.MAX_VALUE) {
        bounds.addProperty("max", range.max());
      }
      state.add(range.name(), bounds);
      root.add("state", state);
      return root;
    }
    if (condition instanceof AllOfCondition all) {
      JsonObject merged = new JsonObject();
      JsonArray blocks = new JsonArray();
      JsonObject state = new JsonObject();
      for (Condition term : all.terms()) {
        JsonObject part = writeBlockPredicate(term);
        if (part.has("blocks")) {
          for (JsonElement e : part.getAsJsonArray("blocks")) {
            blocks.add(e);
          }
        }
        if (part.has("state")) {
          for (var e : part.getAsJsonObject("state").entrySet()) {
            state.add(e.getKey(), e.getValue());
          }
        }
      }
      if (blocks.size() > 0) {
        merged.add("blocks", blocks);
      }
      if (state.size() > 0) {
        merged.add("state", state);
      }
      if (merged.size() == 0) {
        throw new IllegalArgumentException("allOf stepping-on block predicate is empty");
      }
      return merged;
    }
    throw new IllegalArgumentException(
        "SteppingOn inner must be a block condition, got " + condition.getClass().getName());
  }

  private static JsonObject weatherJson(WeatherState state) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:weather_check");
    switch (state) {
      case THUNDERING -> root.addProperty("thundering", true);
      case RAINING -> root.addProperty("raining", true);
      case CLEAR -> {
        root.addProperty("raining", false);
        root.addProperty("thundering", false);
      }
    }
    return root;
  }

  private static JsonObject playerResourceJson(PlayerResourceCondition resource) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:player_resource");
    root.addProperty("resource", resource.type().name().toLowerCase(Locale.ROOT));
    root.addProperty("operator", operatorName(resource.operator()));
    root.addProperty("value", resource.expected());
    return root;
  }

  private static JsonObject jobJson(JobCondition job) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "modularjobs:job");
    JsonArray jobs = new JsonArray();
    for (String key : job.jobKeys()) {
      jobs.add(key);
    }
    root.add("jobs", jobs);
    return root;
  }

  private static JsonObject equipmentAllOfJson(List<EquipmentItemCondition> terms) {
    JsonObject slots = new JsonObject();
    for (EquipmentItemCondition term : terms) {
      if (slots.has(term.slot().jsonName())) {
        throw new IllegalArgumentException(
            "Duplicate equipment slot in all_of: " + term.slot().jsonName());
      }
      slots.add(term.slot().jsonName(), equipmentSlotObject(term));
    }
    JsonObject predicate = new JsonObject();
    predicate.add("equipment", slots);
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:entity_properties");
    root.addProperty("entity", "this");
    root.add("predicate", predicate);
    return root;
  }

  private static JsonObject allOfJson(List<JsonElement> terms) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:all_of");
    JsonArray array = new JsonArray();
    for (JsonElement term : terms) {
      array.add(term);
    }
    root.add("terms", array);
    return root;
  }

  private static JsonObject anyOfJson(List<JsonElement> terms) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:any_of");
    JsonArray array = new JsonArray();
    for (JsonElement term : terms) {
      array.add(term);
    }
    root.add("terms", array);
    return root;
  }

  private static JsonObject invertedJson(JsonElement term) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", "minecraft:inverted");
    root.add("term", term);
    return root;
  }

  private static JsonObject modular(String id, java.util.function.Consumer<JsonObject> fields) {
    JsonObject root = new JsonObject();
    root.addProperty("condition", id);
    fields.accept(root);
    return root;
  }

}
