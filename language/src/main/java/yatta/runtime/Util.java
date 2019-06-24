package yatta.runtime;

public final class Util {
  private Util() {}

  public static short int16Read(final byte[] src, int offset) {
    return (short) ((src[offset] << 8) | (src[++offset] & 0xff));
  }

  public static void int16Write(final short val, final byte[] dst, int offset) {
    dst[offset] = (byte) (val >> 8);
    dst[++offset] = (byte) val;
  }

  public static int int32Read(final byte[] src, int offset) {
    return (src[offset] << 24) | ((src[++offset] & 0xff) << 16) | ((src[++offset] & 0xff) <<  8) | (src[++offset] & 0xff);
  }

  public static void int32Write(int value, final byte[] destination, int offset) {
    destination[offset] = (byte)(value >> 24);
    destination[++offset] = (byte)(value >> 16);
    destination[++offset] = (byte)(value >> 8);
    destination[++offset] = (byte) value;
  }

  public static long varInt63Read(final byte[] source, int offset) {
    long result = 0;
    byte piece;
    for (int shift = 0; shift <= 63; shift += 7) {
      piece = source[offset++];
      result |= (0x7fL & piece) << shift;
      if ((0x80 & piece) == 0) break;
    }
    return result;
  }

  public static int varInt63Len(long value) {
    int result = 1;
    while ((0xffffffffffffff80L & value) != 0) {
      result++;
      value >>>= 7;
    }
    return result;
  }

  public static void varInt63Write(long value, byte[] destination, int offset) {
    while ((0xffffffffffffff80L & value) != 0) {
      destination[offset++] = (byte) ((0x7f & value) | 0x80);
      value >>>= 7;
    }
    destination[offset] = (byte) value;
  }

  public static boolean nthBit(final short value, final int n) {
    return ((value << n) & 0x8000) != 0;
  }

  public static int codePointAt(byte[] array, int offset) {
    final byte b0 = array[offset++];
    if ((0x80 & b0) == 0) return b0;
    final byte b1 = array[offset++];
    if ((0xe0 & b0) == 0xc0) return ((0x1f & b0) << 6) | (0x3f & b1);
    final byte b2 = array[offset++];
    if ((0xf0 & b0) == 0xe0) return ((0x0f & b0) << 12) | ((0x3f & b1) << 6) | (0x3f & b2);
    final byte b3 = array[offset];
    return ((0x7 & b0) << 18) | ((0x3f & b1) << 12) | ((0x3f & b2) << 6) | (0x3f & b3);
  }

  public static int codePointLen(int codePoint) {
    if (codePoint < 0x80) return 1;
    if (codePoint < 0x800) return 2;
    if (codePoint < 0x10000) return 3;
    if (codePoint < 0x10ffff) return 4;
    throw new AssertionError();
  }
























  public static int varIntLen(int value) {
    for (int i = 1; i < 5; i++) {
      if (((0xffffffff << (7 * i)) & value) == 0) return i;
    }
    return 5;
  }

  public static void varIntWrite(int value, byte[] destination, int offset) {
    while ((0xffffff80 & value) != 0) {
      destination[offset++] = (byte) ((0x7f & value) | 0x80);
      value >>>= 7;
    }
    destination[offset] = (byte) value;
  }

  public static int varIntRead(byte[] source, int offset) {
    int result = 0;
    byte piece;
    for (int shift = 0; shift <= 28; shift += 7) {
      piece = source[offset++];
      result |= (0x7f & piece) << shift;
      if ((0x80 & piece) == 0) break;
    }
    return result;
  }
}
