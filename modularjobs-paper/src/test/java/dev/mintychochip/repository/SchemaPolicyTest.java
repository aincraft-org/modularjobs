package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

/** Schema ownership: every supported backend is externally provisioned. */
class SchemaPolicyTest {

  @Test
  void neverAppliesSchemaOnConnect() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.MYSQL, null));
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.MYSQL, section));
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.SQLITE, null));
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.SQLITE, section));
  }

  @Test
  void alwaysVerifiesPresence() {
    assertTrue(SchemaPolicy.shouldVerifySchemaPresent(DatabaseType.MYSQL));
    assertTrue(SchemaPolicy.shouldVerifySchemaPresent(DatabaseType.SQLITE));
  }

  @Test
  void autoSchemaFlagIsMisconfiguration() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertTrue(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.MYSQL, section));
    assertFalse(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.MYSQL, null));
    assertTrue(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.SQLITE, section));
    assertFalse(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.SQLITE, null));
  }
}
