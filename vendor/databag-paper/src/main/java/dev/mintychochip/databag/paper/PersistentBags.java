package dev.mintychochip.databag.paper;

import dev.mintychochip.databag.DataBag;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Embeds a {@link DataBag} on an item as a PDC {@code BYTE_ARRAY}.
 *
 * <p>Non-instantiable utility; {@link #write} overwrites any prior value at the key.
 */
public final class PersistentBags {

  private PersistentBags() {}

  /**
   * Writes a serialized {@link DataBag} to an item's persistent data container.
   *
   * @param stack item receiving the serialized bag
   * @param key persistent-data key under which the bag is stored
   * @param bag bag to serialize and store
   */
  public static void write(ItemStack stack, NamespacedKey key, DataBag bag) {
    java.util.Objects.requireNonNull(stack, "stack");
    java.util.Objects.requireNonNull(key, "key");
    java.util.Objects.requireNonNull(bag, "bag");
    byte[] bytes = bag.toBytes();
    stack.editPersistentDataContainer(pdc -> {
      pdc.set(key, PersistentDataType.BYTE_ARRAY, bytes);
    });
  }

  /**
   * Reads a serialized {@link DataBag} from an item's persistent data container.
   *
   * @param stack item to read from
   * @param key persistent-data key storing the bag
   * @return the decoded bag, or empty when the key is missing or stores a null or empty byte array
   * @throws dev.mintychochip.databag.UnknownBagFormatException when stored bytes use an unsupported
   *     envelope version
   */
  public static Optional<DataBag> read(ItemStack stack, NamespacedKey key) {
    java.util.Objects.requireNonNull(stack, "stack");
    java.util.Objects.requireNonNull(key, "key");
    var pdc = stack.getPersistentDataContainer();
    if (!pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
      return Optional.empty();
    }
    byte[] blob = pdc.get(key, PersistentDataType.BYTE_ARRAY);
    if (blob == null || blob.length == 0) {
      return Optional.empty();
    }
    return Optional.of(DataBag.fromBytes(blob));
  }
}
