package dev.mintychochip.databag;

final class Tags {

  static final byte BOOL = 1;
  static final byte INT = 2;
  static final byte LONG = 3;
  static final byte FLOAT = 4;
  static final byte DOUBLE = 5;
  static final byte STRING = 6;
  static final byte BYTES = 7;
  static final byte BYTE = 8;
  static final byte SHORT = 9;
  static final byte INTS = 10;
  static final byte LONGS = 11;
  static final byte UUID = 12;
  static final byte FORMATTED = 13;

  private Tags() {}

  static boolean known(byte tag) {
    return switch (tag) {
      case BOOL, INT, LONG, FLOAT, DOUBLE, STRING, BYTES, BYTE, SHORT, INTS, LONGS, UUID,
          FORMATTED -> true;
      default -> false;
    };
  }

  /**
   * Returns the fixed payload size in bytes for V3 per-entry direct encoding, or -1
   * when the type is variable-length or grouped (booleans).
   *
   * <p>Kryo's single {@code writeInt/writeLong(value, false)} use variable-length
   * zig-zag encoding, so INT, LONG and UUID (two LONGs) are not fixed. Only the
   * primitive types that always write the same number of bytes are fixed here.
   */
  static int fixedPayloadSize(byte tag) {
    return switch (tag) {
      case BYTE -> 1;
      case SHORT -> 2;
      case FLOAT -> 4;
      case DOUBLE -> 8;
      default -> -1;
    };
  }
}
