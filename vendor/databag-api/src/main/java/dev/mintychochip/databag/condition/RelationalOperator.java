package dev.mintychochip.databag.condition;

import java.math.BigDecimal;
import java.util.function.BiPredicate;

/**
 * Numeric comparison over {@link BigDecimal} magnitude (scale ignored).
 */
public enum RelationalOperator implements BiPredicate<BigDecimal, BigDecimal> {

  /** Strictly less than ({@code <}). */
  LESS_THAN((a, b) -> a.compareTo(b) < 0),
  /** Less than or equal ({@code <=}). */
  LESS_THAN_OR_EQUAL((a, b) -> a.compareTo(b) <= 0),
  /** Strictly greater than ({@code >}). */
  GREATER_THAN((a, b) -> a.compareTo(b) > 0),
  /** Greater than or equal ({@code >=}). */
  GREATER_THAN_OR_EQUAL((a, b) -> a.compareTo(b) >= 0),
  /** Equal ({@code ==}). */
  EQUAL((a, b) -> a.compareTo(b) == 0),
  /** Not equal ({@code !=}). */
  NOT_EQUAL((a, b) -> a.compareTo(b) != 0);

  private final BiPredicate<BigDecimal, BigDecimal> predicate;

  RelationalOperator(BiPredicate<BigDecimal, BigDecimal> predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(BigDecimal a, BigDecimal b) {
    return predicate.test(a, b);
  }
}
