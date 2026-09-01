package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class ConnectionSourceFactoryLifecycleTest {

  @Test
  void runtimeSchemaFailureClosesSourceBeforeOwnershipTransfer() {
    SchemaPresence.SchemaMissingException failure =
        new SchemaPresence.SchemaMissingException(DatabaseType.MYSQL, List.of("job_progression"));
    FailingConnectionSource source = new FailingConnectionSource(failure);

    SchemaPresence.SchemaMissingException thrown =
        assertThrows(
            SchemaPresence.SchemaMissingException.class,
            () -> ConnectionSourceFactory.verifySchemaOrClose(source, DatabaseType.MYSQL));

    assertSame(failure, thrown);
    assertTrue(source.shutdown);
  }

  @Test
  void sqlSchemaFailureClosesSourceAndPreservesCause() {
    SQLException failure = new SQLException("database unavailable");
    FailingConnectionSource source = new FailingConnectionSource(failure);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> ConnectionSourceFactory.verifySchemaOrClose(source, DatabaseType.MYSQL));

    assertSame(failure, thrown.getCause());
    assertTrue(source.shutdown);
  }

  private static final class FailingConnectionSource implements ConnectionSource {
    private final Throwable failure;
    private boolean shutdown;

    private FailingConnectionSource(@NotNull Throwable failure) {
      this.failure = failure;
    }

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public @NotNull DatabaseType getType() {
      return DatabaseType.MYSQL;
    }

    @Override
    public boolean isClosed() {
      return shutdown;
    }

    @Override
    public @NotNull Connection getConnection() throws SQLException {
      if (failure instanceof SQLException sqlFailure) {
        throw sqlFailure;
      }
      throw (RuntimeException) failure;
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }
}
