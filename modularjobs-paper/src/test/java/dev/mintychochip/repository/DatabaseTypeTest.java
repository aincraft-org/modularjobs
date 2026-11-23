package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DatabaseTypeTest {

  @Test
  void defaultIsMysql() {
    assertEquals(DatabaseType.MYSQL, DatabaseType.getDefault());
  }

  @Test
  void acceptsSupportedTypesCaseInsensitively() {
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("mysql"));
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("MYSQL"));
    assertEquals(DatabaseType.SQLITE, DatabaseType.fromIdentifier("sqlite"));
    assertEquals(DatabaseType.SQLITE, DatabaseType.fromIdentifier("SQLITE"));
  }

  @Test
  void blankIdentifierDefaultsToMysql() {
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier(null));
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier(""));
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("  "));
  }

  @Test
  void rejectsUnsupportedIdentifiers() {
    for (String id : new String[] {"postgres", "postgresql", "mariadb", "mongo", "h2"}) {
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromIdentifier(id));
      assertTrue(
          ex.getMessage().toLowerCase().contains("sqlite"),
          "message should list supported types for " + id + ": " + ex.getMessage());
    }
  }

  @Test
  void exposesSupportedDrivers() {
    assertEquals("mysql", DatabaseType.MYSQL.getIdentifier());
    assertEquals("com.mysql.cj.jdbc.Driver", DatabaseType.MYSQL.getClassName());
    assertEquals("sqlite", DatabaseType.SQLITE.getIdentifier());
    assertEquals("org.sqlite.JDBC", DatabaseType.SQLITE.getClassName());
    assertEquals(2, DatabaseType.values().length);
  }
}
