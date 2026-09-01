package dev.mintychochip.container;

import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

/**
 * A reward amount paired with the type of currency or experience it grants.
 *
 * @param type how the payable is paid out
 * @param amount the reward quantity and its currency
 */
@NonExtendable
public record Payable(@NotNull PayableType type, @NotNull PayableAmount amount) {}
