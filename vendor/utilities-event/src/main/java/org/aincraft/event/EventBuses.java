package org.aincraft.event;

import org.aincraft.api.event.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public final class EventBuses {

  private EventBuses() {}

  public static @NotNull EventBus create() {
    return new SimpleEventBus();
  }

  public static @NotNull EventBus create(@NotNull Executor asyncExecutor) {
    return new SimpleEventBus(asyncExecutor);
  }
}
