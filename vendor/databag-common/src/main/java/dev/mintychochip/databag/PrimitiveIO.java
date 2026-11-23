package dev.mintychochip.databag;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.UUID;

/**
 * Encodes a single tagged value. Used as the v0 body and as the length-prefixed
 * payload inside v1+ entries.
 */
final class PrimitiveIO {

  private PrimitiveIO() {}

  static void write(Output output, byte tag, Object value) {
    switch (tag) {
      case Tags.BOOL -> output.writeBoolean((Boolean) value);
      case Tags.INT -> output.writeInt((Integer) value, false);
      case Tags.LONG -> output.writeLong((Long) value, false);
      case Tags.FLOAT -> output.writeFloat((Float) value);
      case Tags.DOUBLE -> output.writeDouble((Double) value);
      case Tags.STRING -> output.writeString((String) value);
      case Tags.BYTES -> {
        byte[] blob = (byte[]) value;
        output.writeVarInt(blob.length, true);
        output.writeBytes(blob);
      }
      case Tags.BYTE -> output.writeByte((Byte) value);
      case Tags.SHORT -> output.writeShort((Short) value);
      case Tags.INTS -> {
        int[] ints = (int[]) value;
        output.writeVarInt(ints.length, true);
        for (int n : ints) {
          output.writeInt(n, false);
        }
      }
      case Tags.LONGS -> {
        long[] longs = (long[]) value;
        output.writeVarInt(longs.length, true);
        for (long n : longs) {
          output.writeLong(n, false);
        }
      }
      case Tags.UUID -> {
        UUID uuid = (UUID) value;
        output.writeLong(uuid.getMostSignificantBits(), false);
        output.writeLong(uuid.getLeastSignificantBits(), false);
      }
      case Tags.FORMATTED -> {
        FormattedBytes formatted = (FormattedBytes) value;
        output.writeVarInt(formatted.format(), true);
        byte[] blob = formatted.value();
        output.writeBytes(blob);
      }
      default -> throw new IllegalStateException("unknown tag " + tag);
    }
  }

  static Object read(Input input, byte tag) {
    return switch (tag) {
      case Tags.BOOL -> input.readBoolean();
      case Tags.INT -> input.readInt(false);
      case Tags.LONG -> input.readLong(false);
      case Tags.FLOAT -> input.readFloat();
      case Tags.DOUBLE -> input.readDouble();
      case Tags.STRING -> input.readString();
      case Tags.BYTES -> {
        int length = input.readVarInt(true);
        yield input.readBytes(length);
      }
      case Tags.BYTE -> input.readByte();
      case Tags.SHORT -> input.readShort();
      case Tags.INTS -> {
        int length = input.readVarInt(true);
        int[] ints = new int[length];
        for (int j = 0; j < length; j++) {
          ints[j] = input.readInt(false);
        }
        yield ints;
      }
      case Tags.LONGS -> {
        int length = input.readVarInt(true);
        long[] longs = new long[length];
        for (int j = 0; j < length; j++) {
          longs[j] = input.readLong(false);
        }
        yield longs;
      }
      case Tags.UUID -> new UUID(input.readLong(false), input.readLong(false));
      case Tags.FORMATTED -> {
        int format = input.readVarInt(true);
        int remaining = input.limit() - input.position();
        yield new FormattedBytes(format, input.readBytes(remaining));
      }
      default -> throw new IllegalArgumentException("unknown tag " + tag);
    };
  }
}
