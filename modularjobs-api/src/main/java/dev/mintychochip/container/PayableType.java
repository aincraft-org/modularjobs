package dev.mintychochip.container;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a keyed kind of {@link Payable} and the handler that pays it out.
 *
 * <p>Extends {@link Keyed}, giving each payable type a unique {@link net.kyori.adventure.key.Key}.
 * Instances are non-extendable and are resolved from the payable-type registry via {@link
 * PayableTypes}.
 */
@NonExtendable
public interface PayableType extends Keyed {

  /**
   * Returns the handler responsible for paying out payables of this type.
   *
   * @return the handler for this payable type
   */
  @Contract(pure = true)
  @NotNull
  PayableHandler handler();
}
