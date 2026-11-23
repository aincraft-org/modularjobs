package dev.mintychochip.databag;

/**
 * The byte[] starts with a DataBag magic header but the envelope version is
 * not readable by this library.
 */
public final class UnknownBagFormatException extends IllegalArgumentException {

  /** Unsupported envelope version read from the header. */
  private final int version;

  /**
   * Constructs the exception for an unsupported envelope version.
   *
   * @param version unsupported envelope version read from the header
   */
  public UnknownBagFormatException(int version) {
    super("unknown DataBag format version " + version);
    this.version = version;
  }

  /** Envelope version that could not be read.
   *
   * @return the unsupported version read from the header
   */
  public int version() {
    return version;
  }
}
