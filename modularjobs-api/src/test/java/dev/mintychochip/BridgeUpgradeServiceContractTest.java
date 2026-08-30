package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.upgrade.UpgradeService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class BridgeUpgradeServiceContractTest {

  @Test
  void exposesAuthoritativeUpgradeServiceAccessor() throws NoSuchMethodException {
    Method accessor = Bridge.class.getMethod("upgradeService");

    assertTrue(Modifier.isAbstract(accessor.getModifiers()));
    assertEquals(UpgradeService.class, accessor.getReturnType());
  }
}
