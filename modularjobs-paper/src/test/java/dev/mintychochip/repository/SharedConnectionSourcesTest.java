package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Proves pool identity keying for shared database connections. */
class SharedConnectionSourcesTest {

  @Test
  void poolIdentityMatchesSameJdbcAndUser() {
    YamlConfiguration a = new YamlConfiguration();
    a.set("type", "mysql");
    a.set("jdbc-url", "jdbc:mysql://localhost:3306/modularjobs");
    a.set("username", "modularjobs");
    a.set("password", "secret");

    YamlConfiguration b = new YamlConfiguration();
    b.set("type", "mysql");
    b.set("jdbc-url", "jdbc:mysql://localhost:3306/modularjobs");
    b.set("username", "modularjobs");
    b.set("password", "other");

    assertEquals(
        SharedConnectionSources.poolIdentity(a),
        SharedConnectionSources.poolIdentity(b),
        "same jdbc-url+username must share pool identity");
  }

  @Test
  void poolIdentityDiffersForDifferentUsers() {
    YamlConfiguration a = new YamlConfiguration();
    a.set("jdbc-url", "jdbc:mysql://localhost:3306/modularjobs");
    a.set("username", "alice");

    YamlConfiguration b = new YamlConfiguration();
    b.set("jdbc-url", "jdbc:mysql://localhost:3306/modularjobs");
    b.set("username", "bob");

    assertNotEquals(
        SharedConnectionSources.poolIdentity(a), SharedConnectionSources.poolIdentity(b));
  }

  @Test
  void sqlitePoolIdentityUsesJdbcFileUrl() {
    YamlConfiguration a = new YamlConfiguration();
    a.set("type", "sqlite");
    a.set("jdbc-url", "jdbc:sqlite:plugins/ModularJobs/modularjobs.db");

    YamlConfiguration sameFile = new YamlConfiguration();
    sameFile.set("type", "sqlite");
    sameFile.set("jdbc-url", "jdbc:sqlite:plugins/ModularJobs/modularjobs.db");

    YamlConfiguration otherFile = new YamlConfiguration();
    otherFile.set("type", "sqlite");
    otherFile.set("jdbc-url", "jdbc:sqlite:plugins/ModularJobs/other.db");

    assertEquals(
        SharedConnectionSources.poolIdentity(a), SharedConnectionSources.poolIdentity(sameFile));
    assertNotEquals(
        SharedConnectionSources.poolIdentity(a), SharedConnectionSources.poolIdentity(otherFile));
  }

  @Test
  void sqlitePoolIdentityPreservesCaseSensitiveFileUrl() {
    YamlConfiguration upperCasePath = new YamlConfiguration();
    upperCasePath.set("type", "sqlite");
    upperCasePath.set("jdbc-url", "jdbc:sqlite:/srv/Data/modularjobs.db");

    YamlConfiguration lowerCasePath = new YamlConfiguration();
    lowerCasePath.set("type", "sqlite");
    lowerCasePath.set("jdbc-url", "jdbc:sqlite:/srv/data/modularjobs.db");

    assertNotEquals(
        SharedConnectionSources.poolIdentity(upperCasePath),
        SharedConnectionSources.poolIdentity(lowerCasePath));
  }
}
