package dev.mintychochip.databag;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bag envelope versions. {@code 0} is the unversioned map body shipped first.
 * {@code 1} prefixes {@link #MAGIC} plus a version byte and length-prefixs
 * each value so unknown tags can be skipped. {@code 2} shares one namespace
 * when every key uses it. {@code 3} groups entries by tag and uses a bitmap
 * for booleans; fixed-size payloads are written without an extra length.
 */
final class Formats {

  static final byte[] MAGIC = {'D', 'B', 'A', 'G'};
  static final int UNVERSIONED = 0;
  static final int V1 = 1;
  static final int V2 = 2;
  static final int V3 = 3;
  static final int CURRENT = V2;

  private Formats() {}

  static boolean hasMagic(byte[] bytes) {
    if (bytes == null || bytes.length < MAGIC.length + 1) {
      return false;
    }
    for (int i = 0; i < MAGIC.length; i++) {
      if (bytes[i] != MAGIC[i]) {
        return false;
      }
    }
    return true;
  }

  static byte[] write(DataBag bag, int format) {
    Output output = new Output(256, -1);
    output.writeBytes(MAGIC);
    output.writeByte((byte) format);
    switch (format) {
      case V1 -> writeV1Body(output, bag);
      case V2 -> writeV2Body(output, bag);
      case V3 -> writeV3Body(output, bag);
      default -> throw new UnknownBagFormatException(format);
    }
    return output.toBytes();
  }

  static byte[] toAutoBytes(DataBag bag, int minSavingsPercent) {
    byte[] v2 = write(bag, V2);
    byte[] v3 = write(bag, V3);

    if (v3.length < v2.length) {
      long saved = (long) (v2.length - v3.length) * 100L;
      long required = (long) v2.length * (long) minSavingsPercent;
      if (saved >= required) {
        return v3;
      }
    }
    return v2;
  }

  static DataBag read(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return DataBag.create();
    }
    try {
      if (hasMagic(bytes)) {
        int version = Byte.toUnsignedInt(bytes[MAGIC.length]);
        if (version != V1 && version != V2 && version != V3) {
          throw new UnknownBagFormatException(version);
        }
        Input input = new Input(bytes, MAGIC.length + 1, bytes.length - MAGIC.length - 1);
        DataBag bag = switch (version) {
          case V1 -> readV1Body(input);
          case V2 -> readV2Body(input);
          case V3 -> readV3Body(input);
          default -> throw new UnknownBagFormatException(version);
        };
        bag.sourceFormat(version);
        return bag;
      }
      DataBag bag = readV0Body(new Input(bytes));
      bag.sourceFormat(UNVERSIONED);
      return bag;
    } catch (UnknownBagFormatException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("malformed DataBag", e);
    }
  }

  static byte[] writeUnversioned(DataBag bag) {
    Output output = new Output(256, -1);
    writeV0Body(output, bag);
    return output.toBytes();
  }

  private static void writeV0Body(Output output, DataBag bag) {
    output.writeVarInt(bag.size(), true);
    for (Map.Entry<String, DataBag.Entry> e : bag.entries()) {
      output.writeString(e.getKey());
      DataBag.Entry entry = e.getValue();
      output.writeByte(entry.tag());
      if (entry.tag() == Tags.FORMATTED) {
        Output payload = new Output(32, -1);
        PrimitiveIO.write(payload, entry.tag(), entry.value());
        byte[] blob = payload.toBytes();
        output.writeVarInt(blob.length, true);
        output.writeBytes(blob);
      } else {
        PrimitiveIO.write(output, entry.tag(), entry.value());
      }
    }
  }

  private static DataBag readV0Body(Input input) {
    DataBag bag = DataBag.create();
    int count = input.readVarInt(true);
    for (int i = 0; i < count; i++) {
      String key = input.readString();
      byte tag = input.readByte();
      if (tag == Tags.FORMATTED) {
        int length = input.readVarInt(true);
        byte[] payload = input.readBytes(length);
        bag.putRaw(key, tag, PrimitiveIO.read(new Input(payload), tag));
      } else {
        bag.putRaw(key, tag, PrimitiveIO.read(input, tag));
      }
    }
    return bag;
  }

  private static void writeV1Body(Output output, DataBag bag) {
    output.writeVarInt(bag.size(), true);
    for (Map.Entry<String, DataBag.Entry> e : bag.entries()) {
      output.writeString(e.getKey());
      DataBag.Entry entry = e.getValue();
      output.writeByte(entry.tag());
      Output payload = new Output(32, -1);
      PrimitiveIO.write(payload, entry.tag(), entry.value());
      byte[] blob = payload.toBytes();
      output.writeVarInt(blob.length, true);
      output.writeBytes(blob);
    }
  }

  private static DataBag readV1Body(Input input) {
    DataBag bag = DataBag.create();
    int count = input.readVarInt(true);
    for (int i = 0; i < count; i++) {
      String key = input.readString();
      byte tag = input.readByte();
      int length = input.readVarInt(true);
      byte[] payload = input.readBytes(length);
      if (!Tags.known(tag)) {
        continue;
      }
      bag.putRaw(key, tag, PrimitiveIO.read(new Input(payload), tag));
    }
    return bag;
  }

  private record NamespaceState(byte mode, String namespace) {}

  private static NamespaceState sharedNamespace(DataBag bag) {
    String common = null;
    for (Map.Entry<String, DataBag.Entry> e : bag.entries()) {
      String key = e.getKey();
      int colon = key.indexOf(':');
      if (colon <= 0 || colon == key.length() - 1) {
        return new NamespaceState((byte) 0, null);
      }
      String ns = key.substring(0, colon);
      if (common == null) {
        common = ns;
      } else if (!common.equals(ns)) {
        return new NamespaceState((byte) 0, null);
      }
    }
    return common == null ? new NamespaceState((byte) 0, null) : new NamespaceState((byte) 1, common);
  }

  private static String keyPart(NamespaceState ns, String key) {
    if (ns.mode == 0) {
      return key;
    }
    int colon = key.indexOf(':');
    return key.substring(colon + 1);
  }

  private static String fullKey(NamespaceState ns, String key) {
    return ns.mode == 0 ? key : ns.namespace + ':' + key;
  }

  private static void writeV2Body(Output output, DataBag bag) {
    output.writeVarInt(bag.size(), true);
    NamespaceState ns = sharedNamespace(bag);
    output.writeByte(ns.mode);
    if (ns.mode == 1) {
      output.writeString(ns.namespace);
    }

    for (Map.Entry<String, DataBag.Entry> e : bag.entries()) {
      output.writeString(keyPart(ns, e.getKey()));
      DataBag.Entry entry = e.getValue();
      output.writeByte(entry.tag());
      Output payload = new Output(32, -1);
      PrimitiveIO.write(payload, entry.tag(), entry.value());
      byte[] blob = payload.toBytes();
      output.writeVarInt(blob.length, true);
      output.writeBytes(blob);
    }
  }

  private static DataBag readV2Body(Input input) {
    DataBag bag = DataBag.create();
    int count = input.readVarInt(true);
    int sharedMode = input.readByte();
    String commonNamespace = null;
    if (sharedMode == 1) {
      commonNamespace = input.readString();
    } else if (sharedMode == 2) {
      // Legacy V2 mode 2: the default namespace was implicit and always "minecraft".
      commonNamespace = "minecraft";
    }
    for (int i = 0; i < count; i++) {
      String key = input.readString();
      key = commonNamespace == null ? key : commonNamespace + ':' + key;
      byte tag = input.readByte();
      int length = input.readVarInt(true);
      byte[] payload = input.readBytes(length);
      if (!Tags.known(tag)) {
        continue;
      }
      bag.putRaw(key, tag, PrimitiveIO.read(new Input(payload), tag));
    }
    return bag;
  }

  private static final class Group {
    final byte tag;
    final List<Map.Entry<String, DataBag.Entry>> entries = new ArrayList<>();

    Group(byte tag) {
      this.tag = tag;
    }
  }

  private static List<Group> buildGroups(DataBag bag) {
    Map<Byte, Group> groups = new LinkedHashMap<>();
    for (Map.Entry<String, DataBag.Entry> e : bag.entries()) {
      groups.computeIfAbsent(e.getValue().tag(), Group::new).entries.add(e);
    }
    return new ArrayList<>(groups.values());
  }

  private static void writeV3Body(Output output, DataBag bag) {
    output.writeVarInt(bag.size(), true);
    NamespaceState ns = sharedNamespace(bag);
    output.writeByte(ns.mode);
    if (ns.mode == 1) {
      output.writeString(ns.namespace);
    }

    List<Group> groups = buildGroups(bag);
    output.writeVarInt(groups.size(), true);
    for (Group group : groups) {
      output.writeByte(group.tag);
      output.writeVarInt(group.entries.size(), true);

      Output groupBody = new Output(64, -1);
      if (group.tag == Tags.BOOL) {
        writeBooleanGroup(groupBody, group, ns);
      } else {
        writeTypedGroup(groupBody, group, ns);
      }

      byte[] body = groupBody.toBytes();
      output.writeVarInt(body.length, true);
      output.writeBytes(body);
    }
  }

  private static void writeBooleanGroup(Output groupBody, Group group, NamespaceState ns) {
    int count = group.entries.size();
    for (Map.Entry<String, DataBag.Entry> e : group.entries) {
      groupBody.writeString(keyPart(ns, e.getKey()));
    }
    int bitmapBytes = (count + 7) / 8;
    byte[] bits = new byte[bitmapBytes];
    for (int i = 0; i < count; i++) {
      Object value = group.entries.get(i).getValue().value();
      if (Boolean.TRUE.equals(value)) {
        bits[i / 8] |= (byte) (1 << (i % 8));
      }
    }
    groupBody.writeBytes(bits);
  }

  private static void writeTypedGroup(Output groupBody, Group group, NamespaceState ns) {
    int fixed = Tags.fixedPayloadSize(group.tag);
    for (Map.Entry<String, DataBag.Entry> e : group.entries) {
      groupBody.writeString(keyPart(ns, e.getKey()));
      if (fixed >= 0) {
        PrimitiveIO.write(groupBody, group.tag, e.getValue().value());
      } else {
        Output payload = new Output(32, -1);
        PrimitiveIO.write(payload, group.tag, e.getValue().value());
        byte[] blob = payload.toBytes();
        groupBody.writeVarInt(blob.length, true);
        groupBody.writeBytes(blob);
      }
    }
  }

  private static DataBag readV3Body(Input input) {
    DataBag bag = DataBag.create();
    int count = input.readVarInt(true);
    NamespaceState ns = readNamespace(input);
    int groupCount = input.readVarInt(true);
    int seen = 0;
    for (int g = 0; g < groupCount; g++) {
      byte tag = input.readByte();
      int groupSize = input.readVarInt(true);
      int groupBytes = input.readVarInt(true);
      if (seen + groupSize > count) {
        input.skip(groupBytes);
        continue;
      }
      if (!Tags.known(tag)) {
        input.skip(groupBytes);
        seen += groupSize;
        continue;
      }
      int start = input.position();
      if (tag == Tags.BOOL) {
        seen += readBooleanGroup(input, bag, groupSize, ns);
      } else {
        seen += readTypedGroup(input, bag, tag, groupSize, ns);
      }
      int consumed = input.position() - start;
      if (consumed < groupBytes) {
        input.skip(groupBytes - consumed);
      } else if (consumed > groupBytes) {
        throw new UnknownBagFormatException(V3);
      }
    }
    return bag;
  }

  private static NamespaceState readNamespace(Input input) {
    int mode = input.readByte();
    if (mode == 1) {
      return new NamespaceState((byte) 1, input.readString());
    }
    return new NamespaceState((byte) 0, null);
  }

  private static int readBooleanGroup(Input input, DataBag bag, int count, NamespaceState ns) {
    List<String> keys = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      keys.add(fullKey(ns, input.readString()));
    }
    int bitmapBytes = (count + 7) / 8;
    byte[] bits = input.readBytes(bitmapBytes);
    for (int i = 0; i < count; i++) {
      boolean value = (bits[i / 8] & (1 << (i % 8))) != 0;
      bag.putRaw(keys.get(i), Tags.BOOL, value);
    }
    return count;
  }

  private static int readTypedGroup(Input input, DataBag bag, byte tag, int count, NamespaceState ns) {
    int fixed = Tags.fixedPayloadSize(tag);
    for (int i = 0; i < count; i++) {
      String key = fullKey(ns, input.readString());
      if (fixed >= 0) {
        byte[] payload = input.readBytes(fixed);
        bag.putRaw(key, tag, PrimitiveIO.read(new Input(payload), tag));
      } else {
        int length = input.readVarInt(true);
        byte[] payload = input.readBytes(length);
        bag.putRaw(key, tag, PrimitiveIO.read(new Input(payload), tag));
      }
    }
    return count;
  }
}
