package dev.mintychochip.domain.model;

import java.math.BigDecimal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable reward record: a payable type, its amount, and optional currency metadata.
 *
 * @param payableTypeKey the type key identifying how the reward is paid
 * @param amount the reward amount
 * @param currencyIdentifier the currency holding the amount, or {@code null} when not
 *     currency-backed
 * @param currencySymbol the persisted display symbol, or {@code null} for currency-less or legacy
 *     rows
 */
public record PayableRecord(
    @NotNull String payableTypeKey,
    @NotNull BigDecimal amount,
    @Nullable String currencyIdentifier,
    @Nullable String currencySymbol) {}
