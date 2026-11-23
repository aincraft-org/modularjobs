package dev.mintychochip.databag;

import java.util.Arrays;
import java.util.Objects;

/**
 * A {@code byte[]} plus a caller-defined format id so payload encodings can
 * migrate without changing the bag key.
 *
 * @param format non-negative format id stored with the bytes
 * @param value payload bytes; defensively copied on construction and on {@link #value()}
 */
public record FormattedBytes(int format, byte[] value) {

  /** Validates the format id and defensively copies the payload bytes. */
  public FormattedBytes {
    if (format < 0) {
      throw new IllegalArgumentException("format must be >= 0");
    }
    value = Objects.requireNonNull(value, "value").clone();
  }

  /** Returns a defensive copy of the payload bytes.
   *
   * @return a copy of the payload bytes
   */
  @Override
  public byte[] value() {
    return value.clone();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FormattedBytes other)) {
      return false;
    }
    return format == other.format && Arrays.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return 31 * Integer.hashCode(format) + Arrays.hashCode(value);
  }
}
