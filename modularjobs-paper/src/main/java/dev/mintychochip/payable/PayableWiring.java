package dev.mintychochip.payable;

import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.ExperiencePayableHandler.ExperienceBarFormatter;
import dev.mintychochip.container.PayableHandler;
import dev.mintychochip.container.PayableRenderer;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.gui.PaperSurfaces;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.service.JobService;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Manual composition for payable types and experience bar (replaces Guice PayableModule). */
public final class PayableWiring {

  private static final String ECONOMY_TYPE = "modularjobs:economy";
  private static final String EXPERIENCE_TYPE = "modularjobs:experience";

  public final @NotNull EconomyProvider economyProvider;
  public final List<Listener> listeners;
  public final @NotNull PayableRenderer renderer;

  private PayableWiring(
      @NotNull EconomyProvider economyProvider,
      @NotNull List<Listener> listeners,
      @NotNull PayableRenderer renderer) {
    this.economyProvider = economyProvider;
    this.listeners = List.copyOf(listeners);
    this.renderer = renderer;
  }

  /**
   * Composes the experience and economy payable handlers, registers the corresponding {@link
   * PayableType}s in {@code payableTypeRegistry}, and resolves the economy provider via {@link
   * EconomyProviderFactory#createOrFail}. Returns the wiring exposing the provider and renderer.
   */
  public static @NotNull PayableWiring create(
      @NotNull Plugin plugin,
      @NotNull JobService jobService,
      @NotNull Registry<PayableType> payableTypeRegistry,
      @NotNull PaperSurfaces surfaces,
      @Nullable ExperienceBarColorPreference experienceBarColorPreference) {
    ExperienceBarColorProvider colorProvider =
        new ExperienceBarColorProvider(experienceBarColorPreference);
    ExperienceBarControllerImpl controller = new ExperienceBarControllerImpl(plugin, surfaces);
    ExperienceBarFormatter formatter = new ExperienceBarFormatterImpl(colorProvider);
    PayableHandler experienceHandler =
        new BufferedExperienceHandlerImpl(controller, formatter, jobService);

    EconomyProvider economyProvider = EconomyProviderFactory.createOrFail(plugin);
    PayableHandler economyHandler = economyHandlerFor(economyProvider);
    PayableRenderer renderer = new PayableRendererImpl();

    payableTypeRegistry.register(economyType(economyHandler));
    payableTypeRegistry.register(experienceType(experienceHandler));

    List<Listener> listeners = List.of(controller);
    return new PayableWiring(economyProvider, listeners, renderer);
  }

  /** Delegates economy payables to the selected provider, including the blackhole fallback. */
  static @NotNull PayableHandler economyHandlerFor(@NotNull EconomyProvider economyProvider) {
    return context -> economyProvider.deposit(context.playerId(), context.payable().amount());
  }

  private static @NotNull PayableType economyType(@NotNull PayableHandler handler) {
    Key key = NamespacedKey.fromString(ECONOMY_TYPE);
    return new PayableType() {
      @Override
      public @NotNull PayableHandler handler() {
        return handler;
      }

      @Override
      public @NotNull Key key() {
        return key;
      }
    };
  }

  private static @NotNull PayableType experienceType(@NotNull PayableHandler handler) {
    Key key = NamespacedKey.fromString(EXPERIENCE_TYPE);
    return new PayableType() {
      @Override
      public @NotNull PayableHandler handler() {
        return handler;
      }

      @Override
      public @NotNull Key key() {
        return key;
      }
    };
  }
}
