package dev.mintychochip;

import java.lang.reflect.Proxy;
import org.aincraft.api.event.EventBus;
import org.jetbrains.annotations.NotNull;

/** Registers a {@link Bridge} that only serves an event bus, for services that publish events. */
public final class BridgeFixture {

  private BridgeFixture() {}

  /** Installs a bridge whose {@code eventBus()} returns {@code bus}; other calls fail. */
  public static void install(@NotNull EventBus bus) {
    BridgeRuntime.register(
        (Bridge)
            Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {Bridge.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("eventBus")) {
                    return bus;
                  }
                  throw new UnsupportedOperationException(method.getName());
                }));
  }

  /** Clears the installed bridge. */
  public static void uninstall() {
    BridgeRuntime.unregister();
  }
}
