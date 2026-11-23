package dev.mintychochip.databag.condition;

import dev.mintychochip.databag.DataBag;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * SPI for a condition id that is not built into {@code dev.mintychochip.databag}.
 * JSON object fields (except {@code condition}/{@code type}) are passed as a
 * {@link DataBag} under the {@code condition} namespace. Snapshot extras for
 * evaluation live on {@link ConditionContext#extras()}.
 */
public interface ConditionHandler {

  /**
   * Namespaced condition id, e.g. {@code acme:party_size}.
   *
   * @return the namespaced condition id owned by this handler
   */
  Key id();
  /**
   * Builds a {@link Condition} from handler-specific JSON arguments.
   *
   * @param arguments decoded object fields for this condition id
   * @return parsed condition
   */
  Condition read(DataBag arguments);

  /**
   * Serializes {@code condition} if this handler owns it. Empty means "not
   * mine" so another handler can try.
   *
   * @param condition condition graph to serialize
   * @return a bag of handler-owned fields, or empty when this handler does not own {@code condition}
   */
  Optional<DataBag> write(Condition condition);
}
