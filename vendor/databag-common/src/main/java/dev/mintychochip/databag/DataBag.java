package dev.mintychochip.databag;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;

/**
 * Light namespaced primitive bag, PDC-shaped but Paper-free. The whole bag
 * encodes to a Kryo {@code byte[]} (boolean, byte, short, int, long, float,
 * double, string, UUID, byte[], int[], long[], formatted bytes). Missing or
 * wrong-typed keys are empty, never thrown.
 *
 * <p>Writes always use envelope v2 ({@link #CURRENT_FORMAT}): {@code DBAG}
 * magic, version byte, and length-prefixed entries with optional shared
 * namespace compression. Reads accept unversioned bags and every known
 * envelope. Unknown envelope versions throw {@link UnknownBagFormatException}.
 * Unknown value tags in v1+ are skipped so new primitives do not need a
 * version bump. Malformed bodies throw {@link IllegalArgumentException}.
 */
public final class DataBag {

  /** Unversioned map body written before the {@code DBAG} header existed. */
  public static final int UNVERSIONED_FORMAT = Formats.UNVERSIONED;

  /** Current write envelope (v2): magic + version + shared-namespace entries. */
  public static final int CURRENT_FORMAT = Formats.CURRENT;

  /** Default minimum savings percent for {@link #toAutoBytes()}. */
  private static final int DEFAULT_SAVINGS_THRESHOLD = 10;
  private final Map<String, Entry> values = new LinkedHashMap<>();
  private int sourceFormat = CURRENT_FORMAT;

  private DataBag() {}

  /** Creates an empty bag that writes in {@link #CURRENT_FORMAT}.
   *
   * @return a new empty bag
   */
  public static DataBag create() {
    return new DataBag();
  }

  /**
   * {@code true} when {@code bytes} start with the versioned DataBag magic.
   * Unversioned (v0) bags return {@code false} — they are still readable.
   *
   * @param bytes candidate byte payload
   * @return {@code true} when the payload starts with the versioned DataBag magic
   */
  public static boolean isVersioned(byte[] bytes) {
    return Formats.hasMagic(bytes);
  }

  /** Envelope this bag was decoded from; {@link #CURRENT_FORMAT} if created fresh.
   *
   * @return the envelope format id this instance was read from
   */
  public int sourceFormat() {
    return sourceFormat;
  }

  /**
   * Returns whether a value is stored at {@code key}, regardless of its type.
   *
   * @param key bag key
   * @return {@code true} when the key is present
   */
  public boolean has(Key key) {
    return values.containsKey(id(key));
  }

  /**
   * Stores a boolean at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   */
  public DataBag setBoolean(Key key, boolean value) {
    values.put(id(key), new Entry(Tags.BOOL, value));
    return this;
  }

  /**
   * Stores a 32-bit integer at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   */
  public DataBag setInt(Key key, int value) {
    values.put(id(key), new Entry(Tags.INT, value));
    return this;
  }

  /**
   * Stores a 64-bit integer at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   */
  public DataBag setLong(Key key, long value) {
    values.put(id(key), new Entry(Tags.LONG, value));
    return this;
  }

  /**
   * Stores a 32-bit float at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   */
  public DataBag setFloat(Key key, float value) {
    values.put(id(key), new Entry(Tags.FLOAT, value));
    return this;
  }

  /**
   * Stores a 64-bit float at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   */
  public DataBag setDouble(Key key, double value) {
    values.put(id(key), new Entry(Tags.DOUBLE, value));
    return this;
  }

  /**
   * Stores a UTF-8 string at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   * @throws NullPointerException if {@code value} is {@code null}
   */
  public DataBag setString(Key key, String value) {
    values.put(id(key), new Entry(Tags.STRING, Objects.requireNonNull(value)));
    return this;
  }

  /**
   * Stores a raw byte array at {@code key}.
   *
   * @param key bag key
   * @param value value to store; defensively copied
   * @return this bag
   */
  public DataBag setBytes(Key key, byte[] value) {
    values.put(id(key), new Entry(Tags.BYTES, Objects.requireNonNull(value, "value").clone()));
    return this;
  }

  /**
   * Stores {@code value} with a caller-defined format id. Use this when the
   * bytes may change encoding later (JSON v1 → JSON v2, binary, …).
   *
   * @param key bag key
   * @param format non-negative format id stored with the bytes
   * @param value payload bytes; defensively copied via {@link FormattedBytes}
   * @return this bag
   */
  public DataBag setBytes(Key key, int format, byte[] value) {
    values.put(id(key), new Entry(Tags.FORMATTED, new FormattedBytes(format, value)));
    return this;
  }

  /**
   * Stores {@code value} using a registered (or caller-held) {@link DataHandler}.
   *
   * @param <T> handler value type
   * @param handler handler that encodes {@code value}
   * @param value value to store
   * @return this bag
   * @throws NullPointerException if {@code handler} is {@code null}
   */
  public <T> DataBag set(DataHandler<T> handler, T value) {
    Objects.requireNonNull(handler, "handler");
    return setBytes(handler.key(), handler.format(), handler.encode(value));
  }

  /**
   * Stores an 8-bit integer at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   */
  public DataBag setByte(Key key, byte value) {
    values.put(id(key), new Entry(Tags.BYTE, value));
    return this;
  }

  /**
   * Stores a 16-bit integer at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   */
  public DataBag setShort(Key key, short value) {
    values.put(id(key), new Entry(Tags.SHORT, value));
    return this;
  }

  /**
   * Stores an {@code int[]} at {@code key}.
   *
   * @param key bag key
   * @param value value to store; defensively copied
   * @return this bag
   */
  public DataBag setInts(Key key, int[] value) {
    values.put(id(key), new Entry(Tags.INTS, Objects.requireNonNull(value, "value").clone()));
    return this;
  }

  /**
   * Stores a {@code long[]} at {@code key}.
   *
   * @param key bag key
   * @param value value to store; defensively copied
   * @return this bag
   */
  public DataBag setLongs(Key key, long[] value) {
    values.put(id(key), new Entry(Tags.LONGS, Objects.requireNonNull(value, "value").clone()));
    return this;
  }

  /**
   * Stores a {@link UUID} at {@code key}.
   *
   * @param key bag key
   * @param value value to store
   * @return this bag
   * @throws NullPointerException if {@code value} is {@code null}
   */
  public DataBag setUuid(Key key, UUID value) {
    values.put(id(key), new Entry(Tags.UUID, Objects.requireNonNull(value)));
    return this;
  }

  /**
   * Reads a boolean at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a boolean
   */
  public Optional<Boolean> getBoolean(Key key) {
    return typed(key, Tags.BOOL, Boolean.class);
  }

  /**
   * Reads a 32-bit integer at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not an integer
   */
  public OptionalInt getInt(Key key) {
    return typed(key, Tags.INT, Integer.class).map(OptionalInt::of).orElseGet(OptionalInt::empty);
  }

  /**
   * Reads a 64-bit integer at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a long
   */
  public OptionalLong getLong(Key key) {
    return typed(key, Tags.LONG, Long.class).map(OptionalLong::of).orElseGet(OptionalLong::empty);
  }

  /**
   * Reads a 32-bit float at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a float
   */
  public Optional<Float> getFloat(Key key) {
    return typed(key, Tags.FLOAT, Float.class);
  }

  /**
   * Reads a 64-bit float at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a double
   */
  public OptionalDouble getDouble(Key key) {
    return typed(key, Tags.DOUBLE, Double.class).map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
  }

  /**
   * Reads a UTF-8 string at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a string
   */
  public Optional<String> getString(Key key) {
    return typed(key, Tags.STRING, String.class);
  }

  /**
   * Reads a raw byte array at {@code key}.
   *
   * @param key bag key
   * @return a defensive copy of the stored value, or empty when the key is missing or not bytes
   */
  public Optional<byte[]> getBytes(Key key) {
    return typed(key, Tags.BYTES, byte[].class).map(byte[]::clone);
  }

  /**
   * Reads formatted bytes at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not formatted bytes
   */
  public Optional<FormattedBytes> getFormatted(Key key) {
    return typed(key, Tags.FORMATTED, FormattedBytes.class);
  }

  /**
   * Reads a typed value with {@code handler}. Empty when the key is missing,
   * the format id does not match, or the slot is not formatted bytes.
   *
   * @param <T> handler value type
   * @param handler handler that decodes the stored bytes
   * @return the decoded value, or empty when the slot cannot be read with {@code handler}
   * @throws NullPointerException if {@code handler} is {@code null}
   */
  public <T> Optional<T> get(DataHandler<T> handler) {
    Objects.requireNonNull(handler, "handler");
    Optional<FormattedBytes> formatted = getFormatted(handler.key());
    if (formatted.isEmpty() || formatted.get().format() != handler.format()) {
      return Optional.empty();
    }
    try {
      return Optional.of(handler.decode(formatted.get().value()));
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
  }

  /** Namespaced keys present in this bag, in insertion order.
   *
   * @return snapshot of the keys in insertion order
   */
  public Set<Key> keys() {
    Set<Key> keys = new LinkedHashSet<>();
    for (String raw : values.keySet()) {
      try {
        keys.add(Key.key(raw));
      } catch (RuntimeException ignored) {
        // Deserialized raw strings that are not Adventure keys stay addressable
        // via putRaw/get with a custom Key; they are omitted from this view.
      }
    }
    return Set.copyOf(keys);
  }

  /**
   * Reads an 8-bit integer at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a byte
   */
  public Optional<Byte> getByte(Key key) {
    return typed(key, Tags.BYTE, Byte.class);
  }

  /**
   * Reads a 16-bit integer at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a short
   */
  public Optional<Short> getShort(Key key) {
    return typed(key, Tags.SHORT, Short.class);
  }

  /**
   * Reads an {@code int[]} at {@code key}.
   *
   * @param key bag key
   * @return a defensive copy of the stored value, or empty when the key is missing or not an int array
   */
  public Optional<int[]> getInts(Key key) {
    return typed(key, Tags.INTS, int[].class).map(int[]::clone);
  }

  /**
   * Reads a {@code long[]} at {@code key}.
   *
   * @param key bag key
   * @return a defensive copy of the stored value, or empty when the key is missing or not a long array
   */
  public Optional<long[]> getLongs(Key key) {
    return typed(key, Tags.LONGS, long[].class).map(long[]::clone);
  }

  /**
   * Reads a {@link UUID} at {@code key}.
   *
   * @param key bag key
   * @return the stored value, or empty when the key is missing or not a UUID
   */
  public Optional<UUID> getUuid(Key key) {
    return typed(key, Tags.UUID, UUID.class);
  }
  /**
   * Kryo-framed byte array of this bag in the requested envelope format.
   *
   * @param format envelope version to write
   * @return encoded bytes in the requested format
   */
  public byte[] toBytes(int format) {
    return Formats.write(this, format);
  }

  /**
   * Kryo-framed byte array of this bag in {@link #CURRENT_FORMAT}. This is the
   * payload written to a Paper PDC as {@code PersistentDataType.BYTE_ARRAY}.
   *
   * @return encoded bytes of this bag in the current envelope format
   */
  public byte[] toBytes() {
    return Formats.write(this, CURRENT_FORMAT);
  }

  /**
   * Kryo-framed byte array of this bag, automatically choosing V2 or V3 based on
   * which produces the smaller payload. The default savings threshold is 10%.
   *
   * @return encoded bytes of this bag in the smaller envelope format
   */
  public byte[] toAutoBytes() {
    return Formats.toAutoBytes(this, DEFAULT_SAVINGS_THRESHOLD);
  }

  /**
   * Kryo-framed byte array of this bag, automatically choosing V2 or V3 based on
   * which produces the smaller payload, subject to the given minimum savings
   * threshold (0..100). Values outside that range are clamped.
   *
   * @param minSavingsPercent minimum savings required to choose V3
   * @return encoded bytes of this bag in the chosen envelope format
   */
  public byte[] toAutoBytes(int minSavingsPercent) {
    if (minSavingsPercent < 0) minSavingsPercent = 0;
    if (minSavingsPercent > 100) minSavingsPercent = 100;
    return Formats.toAutoBytes(this, minSavingsPercent);
  }

  /**
   * Decodes a bag from Kryo-framed bytes.
   *
   * @param bytes encoded bag bytes
   * @return a bag containing the decoded entries
   * @throws UnknownBagFormatException when the bytes use an unsupported envelope version
   */
  public static DataBag fromBytes(byte[] bytes) {
    return Formats.read(bytes);
  }

  int size() {
    return values.size();
  }

  Iterable<Map.Entry<String, Entry>> entries() {
    return values.entrySet();
  }

  void putRaw(String key, byte tag, Object value) {
    values.put(key, new Entry(tag, value));
  }

  void sourceFormat(int version) {
    this.sourceFormat = version;
  }

  private <T> Optional<T> typed(Key key, byte tag, Class<T> type) {
    Entry entry = values.get(id(key));
    if (entry == null || entry.tag != tag) {
      return Optional.empty();
    }
    return Optional.of(type.cast(entry.value));
  }

  private static String id(Key key) {
    return Objects.requireNonNull(key, "key").asString();
  }

  record Entry(byte tag, Object value) {}
}
