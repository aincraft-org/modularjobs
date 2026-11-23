package dev.mintychochip.databag.condition;

import dev.mintychochip.databag.DataBag;

import java.nio.charset.StandardCharsets;

/**
 * Reader/writer for {@link Condition} graphs. Bytes are vanilla loot-condition
 * JSON (UTF-8). This is the conditions-API replacement for Kryo codecs.
 */
public interface ConditionSerializer {

  /** Format id stored with JSON condition payloads in a {@link DataBag} formatted slot. */
  int JSON_FORMAT = 1;

  /**
   * Serializes {@code condition} to vanilla-shaped JSON bytes.
   *
   * @param condition condition graph to serialize
   * @return UTF-8 vanilla-shaped JSON bytes
   */
  byte[] write(Condition condition);

  /**
   * Parses vanilla-shaped JSON bytes into a {@link Condition}.
   *
   * <p>Malformed JSON parse exceptions are propagated by the implementation.</p>
   *
   * @param bytes UTF-8 vanilla-shaped JSON bytes
   * @return parsed condition graph
   * @throws IllegalArgumentException when the payload is not an object or the
   *     condition id is unknown or malformed
   */
  Condition read(byte[] bytes);

  /**
   * Serializes {@code condition} to vanilla-shaped JSON text.
   *
   * <p>The default implementation decodes the bytes returned by {@link #write(Condition)}
   * as UTF-8. Implementations that already produce text can override this method to avoid
   * the intermediate byte array.</p>
   *
   * @param condition condition graph to serialize
   * @return UTF-8 JSON text
   */
  default String writeJson(Condition condition) {
    return new String(write(condition), StandardCharsets.UTF_8);
  }

  /**
   * Parses vanilla-shaped JSON text into a {@link Condition}.
   *
   * <p>The default implementation encodes {@code json} as UTF-8 and delegates to
   * {@link #read(byte[])}.</p>
   *
   * <p>Malformed JSON parse exceptions are propagated by the implementation.</p>
   *
   * @param json UTF-8 JSON text
   * @return parsed condition graph
   * @throws IllegalArgumentException when the payload is not an object or the
   *     condition id is unknown or malformed
   */
  default Condition readJson(String json) {
    return read(json.getBytes(StandardCharsets.UTF_8));
  }
}
