package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertSame;

import dev.mintychochip.upgrade.UpgradeService;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class BridgeImplUpgradeServiceTest {

  @Test
  void returnsTheExactUpgradeServiceInstanceProvidedToConstructor() {
    UpgradeService expected =
        (UpgradeService)
            Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {UpgradeService.class},
                (proxy, method, args) -> null);

    Bridge bridge =
        new BridgeImpl(
            null, null, null, expected, null, null, null, null, null, null, null, null, null);

    assertSame(expected, bridge.upgradeService());
  }
}
