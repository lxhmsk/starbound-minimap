package starbound.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class BTreeDB5 {

  private static final int HEADER_SIZE = 512;
  private static final short BLOCK_TYPE_FREE = 'F' | 'F' << 8;
  private static final short BLOCK_TYPE_INDEX = 'I' | 'I' << 8;
  private static final short BLOCK_TYPE_LEAF = 'L' | 'L' << 8;
  
  public static BTreeDB5 load(File file) throws IOException {

    ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
    
    String magic = readNullPaddedString(data, 8);
    if (!magic.equals("BTreeDB5")) {
      throw new AssertionError("Not a BTreeDB5: " + magic);
    }
    int blockSize = data.getInt();
    String name = readNullPaddedString(data, 16);
    int keySize = data.getInt();
    boolean useOtherRootBlockIndex = data.get() > 0;
    int lastBlockIndex = data.getInt();
    data.get(); // unknown
    data.get(); // unknown;
    data.get(); // unknown;
    data.getInt(); // stats?
    data.get(); // unknown
    int rootBlockIndex = data.getInt();
    data.get(); // unknown
    data.getInt(); // free block index
    data.get(); // unknown
    data.get(); // unknown
    data.get(); // unknown
    data.getInt(); // stats?
    data.get(); // unknown
    int otherRoot = data.getInt();
    skipBytes(data, 446);
    if (data.position() != HEADER_SIZE) {
      throw new AssertionError();
    }

    return new BTreeDB5(
        data, blockSize, name, keySize, lastBlockIndex + 1, rootBlockIndex, otherRoot,
        useOtherRootBlockIndex);
  }
  
  /**
   * Reads a null padded string (null padding is after the string).
   */
  private static String readNullPaddedString(ByteBuffer data, int length) {
    byte[] bytes = new byte[length];
    data.get(bytes);
    int stringLength = bytes.length - 1;
    while (stringLength > 0 && bytes[stringLength] == 0) {
      stringLength--;
    }
    return new String(bytes, 0, stringLength+1, Charset.forName("utf-8"));
  }
  
  private static void skipBytes(ByteBuffer data, int n) {
    data.position(data.position() + n);
  }

  private final ByteBuffer data;

  public final int blockSize;
  public final String name;
  public final int keySize;
  @SuppressWarnings("unused")
  private final int blockCount;
  private final int rootBlockIndex;
  private final int otherRootBlockIndex;
  private final boolean useOtherRootBlockIndex;

  private final byte[] tempKey;
  
  private BTreeDB5(ByteBuffer data, int blockSize, String name, int keySize, int blockCount,
      int rootBlockIndex, int otherRoot, boolean useOtherRootBlockIndex) {
    this.data = data;
    this.blockSize = blockSize;
    this.name = name;
    this.keySize = keySize;
    this.blockCount = blockCount;
    this.rootBlockIndex = rootBlockIndex;
    this.otherRootBlockIndex = otherRoot;
    this.useOtherRootBlockIndex = useOtherRootBlockIndex;
    
    this.tempKey = new byte[keySize]; 
  }

  static String toString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString();
  }

  public List<byte[]> getKeys() {
    ArrayList<byte[]> keys = new ArrayList<>();
    getKeys(keys, getRootBlockIndex());
    return keys;
  }

  public List<byte[]> getAlternateKeys() {
    ArrayList<byte[]> keys = new ArrayList<>();
    getKeys(keys, otherRootBlockIndex);
    return keys;
  }
  
  private void getKeys(List<byte[]> keys, int blockIndex) {

    int offset = getBlockOffset(blockIndex);
    data.position(offset);
    short blockType = data.getShort();
    
    if (blockType == BLOCK_TYPE_INDEX) {

      data.get(); // unknown?
      int numEntries = data.getInt();
      int block = data.getInt();

      final int entrySize = keySize + 4;
      int entryOffset = data.position();

      getKeys(keys, block);
      for (int entryIndex = 0; entryIndex < numEntries; entryIndex++) {
        data.position(entryOffset + entryIndex * entrySize);
        data.get(tempKey);
        block = data.getInt();
        getKeys(keys, block);
      }

    } else if (blockType == BLOCK_TYPE_LEAF) {
      
      LeafReader leafReader = new LeafReader();
      int numKeys = leafReader.readInt();
      for (int i = 0; i < numKeys; i++) {
        ByteBuffer key = leafReader.read(keySize);
        keys.add(key.array());
        int dataLength = leafReader.readVarint();
        leafReader.skip(dataLength);
      }
      
    } else if (blockType == BLOCK_TYPE_FREE) {
      throw new AssertionError("Should not find free blocks in index");
    } else {
      throw new AssertionError(String.format("Unknown block type: 0x%02X", blockType));
    }
  }
  
  private int getBlockOffset(int blockIndex) {
    return HEADER_SIZE + blockSize * blockIndex;
  }
  
  public ByteBuffer get(long key) {
    byte[] bytes = new byte[keySize];
    for (int i = keySize - 1; i >= 0; i--) {
      bytes[i] = (byte)(key & 0xFF);
      key = key >> 8;
    }
    return get(bytes);
  }
  
  public ByteBuffer get(byte[] key) {
    if (key.length != keySize) {
      throw new AssertionError("Invalid key size");
    }
    int offset = getBlockOffset(getRootBlockIndex());
    int entrySize = keySize + 4;
    short blockType;
    while (true) {
      data.position(offset);
      blockType = data.getShort();
      if (blockType != BLOCK_TYPE_INDEX) {
        break;
      }
      int lo = 0;
      data.get(); // unknown?
      int hi = data.getInt();
      int block = data.getInt();
      offset += 11; // short + byte + int + int = 11 bytes
      while (lo < hi) {
        int mid = (lo + hi) / 2;
        data.position(offset + entrySize * mid);
        data.get(tempKey);
        if (compare(key, tempKey) < 0) {
          hi = mid;
        } else {
          lo = mid + 1;
        }
      }
      if (lo > 0) {
        data.position(offset + entrySize * (lo - 1) + keySize);
        block = data.getInt();
      }
      offset = getBlockOffset(block);
    }
    if (blockType != BLOCK_TYPE_LEAF) {
      throw new AssertionError("Did not reach a leaf");
    }
    
    LeafReader leafReader = new LeafReader();
    int numKeys = leafReader.readInt();
    for (int i = 0; i < numKeys; i++) {
      ByteBuffer currentKey = leafReader.read(keySize);
      int length = leafReader.readVarint();
      if (compare(key, currentKey.array()) == 0) {
        return leafReader.read(length);
      }
      leafReader.skip(length);
    }
    return null;
  }

  private static int compare(byte[] a, byte[] b) {
    if (a.length != b.length) {
      throw new AssertionError("Cannot compare keys of mismatched sized");
    }
    for (int i = 0; i < a.length; i++) {
      // Need to compare unsigned
      int c = Integer.compare(a[i] & 0xFF, b[i] & 0xFF);
      if (c != 0) {
        return c;
      }
    }
    return 0;
  }
  
  private class LeafReader {

    private int offset = 2;

    private int readVarint() {
      int value = 0;
      for (int i = 0; i < 4; i++) {
        byte b = read(1).get(0);
        if ((b & 0b1000_0000) == 0) {
          return value << 7 | b;
        }
        value = value << 7 | (b & 0b0111_1111);
      }
      throw new AssertionError("Varint larger than 4 bytes");
    }
    
    private int readInt() {
      offset += 4;
      return data.getInt();
    }
    
    private ByteBuffer read(int length) {
      ByteBuffer bytes = ByteBuffer.allocate(length);
      _read(bytes, length);
      bytes.position(0);
      return bytes;
    }
    
    private ByteBuffer skip(int length) {
      return _read(null, length);
    }
    
    private ByteBuffer _read(ByteBuffer bytes, int length) {

      final int blockEnd = blockSize - 4;
      while (true) {
        if (offset + length <= blockEnd) {
          for (int i = 0; i < length; i++) {
            byte b = data.get();
            if (bytes != null) {
              bytes.put(b);
            }
          }
          offset += length;
          break;
        }
        int delta = blockEnd - offset;
        for (int i = 0; i < delta; i++) {
          byte b = data.get();
          if (bytes != null) {
            bytes.put(b);
          }
        }
        int nextBlock = data.getInt();
        if (nextBlock < 0) {
          throw new AssertionError("Could not traverse to next block");
        }
        data.position(getBlockOffset(nextBlock));
        if (data.getShort() != BLOCK_TYPE_LEAF) {
          throw new AssertionError("Did not reach a leaf");
        }
        offset = 2;
        length -= delta;
      }

      return bytes;
    }
  }

  private int getRootBlockIndex() {
    return useOtherRootBlockIndex ? otherRootBlockIndex : rootBlockIndex;
  }
}
