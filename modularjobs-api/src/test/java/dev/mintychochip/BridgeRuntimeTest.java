package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BridgeRuntimeTest {

  @AfterEach
  void clearRuntime() {
    BridgeRuntime.unregister();
  }

  @Test
  void exposesOnlyTheRegisteredBridgeThroughThePublicLookup() {
    Bridge registered =
        (Bridge)
            Proxy.newProxyInstance(
                Bridge.class.getClassLoader(),
                new Class<?>[] {Bridge.class},
                (proxy, method, arguments) -> null);

    BridgeRuntime.register(registered);

    assertSame(registered, Bridge.bridge());
  }

  @Test
  void publicLookupFailsWhenThePluginHasNotRegisteredABridge() {
    BridgeRuntime.unregister();

    assertThrows(IllegalStateException.class, Bridge::bridge);
  }
}
