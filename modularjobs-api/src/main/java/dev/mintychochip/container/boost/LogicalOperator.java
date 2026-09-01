package dev.mintychochip.container.boost;

import java.util.function.BiPredicate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A boolean logic operator applied to two boolean operands.
 *
 * <p>Each constant wraps a {@link BiPredicate} with the corresponding boolean semantics over two
 * {@code boolean} values.
 */
public enum LogicalOperator implements BiPredicate<Boolean, Boolean> {
  AND((a, b) -> a && b),
  OR((a, b) -> a || b),
  XOR((a, b) -> a ^ b),
  NAND((a, b) -> !(a && b)),
  NOR((a, b) -> !(a || b)),
  XNOR((a, b) -> a.booleanValue() == b.booleanValue()),
  IMPLIES((a, b) -> !a || b);

  private final BiPredicate<Boolean, Boolean> predicate;

  LogicalOperator(BiPredicate<Boolean, Boolean> predicate) {
    this.predicate = predicate;
  }

  @Override
  @Contract(pure = true)
  public boolean test(@NotNull Boolean a, @NotNull Boolean b) {
    return predicate.test(a, b);
  }
}
