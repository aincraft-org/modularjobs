package dev.mintychochip.payable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableRenderer;
import dev.mintychochip.container.PayableType;
import java.math.BigDecimal;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class PayableRendererImplTest {

  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final PayableRenderer renderer = new PayableRendererImpl();

  @Test
  void rendersEconomyPayablesWithoutDomainRenderingLogic() {
    Payable payable =
        new Payable(payableType("economy"), PayableAmount.create(new BigDecimal("1234.5")));

    assertEquals("$1,234.50", PLAIN.serialize(renderer.render(payable)));
  }

  @Test
  void rendersExperiencePayablesWithoutDomainRenderingLogic() {
    Payable payable =
        new Payable(payableType("experience"), PayableAmount.create(new BigDecimal("12.5")));

    assertEquals("12.50xp", PLAIN.serialize(renderer.render(payable)));
  }

  @Test
  void rejectsPayablesWithoutRegisteredPresentation() {
    Payable payable = new Payable(payableType("custom"), PayableAmount.create(BigDecimal.ONE));

    assertThrows(IllegalArgumentException.class, () -> renderer.render(payable));
  }

  private static @NotNull PayableType payableType(@NotNull String value) {
    Key key = Key.key("modularjobs", value);
    return new PayableType() {
      @Override
      public @NotNull PayableHandler handler() {
        return ignored -> {};
      }

      @Override
      public @NotNull Key key() {
        return key;
      }
    };
  }
}
