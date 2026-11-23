package dev.mintychochip.databag;

import net.kyori.adventure.key.Key;

/**
 * Codec for a typed value stored at a namespaced key. Plugins register
 * handlers so {@link DataBag} can carry domain objects without knowing those
 * types.
 *
 * @param <T> value type this codec reads and writes
 */
public interface DataHandler<T> {

  /**
   * Bag key this handler reads and writes.
   *
   * @return the namespaced bag key
   */
  Key key();

  /**
   * Payload format id stored with the bytes (see {@link DataBag#setBytes(Key, int, byte[])}).
   *
   * @return the payload format id
   */
  int format();

  /**
   * Serializes {@code value} to bytes stored in the bag.
   *
   * @param value value to encode
   * @return encoded bytes
   */
  byte[] encode(T value);

  /**
   * Deserializes bytes read from the bag.
   *
   * @param bytes encoded value bytes
   * @return decoded value
   */
  T decode(byte[] bytes);
}
