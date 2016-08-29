package starbound.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SBAsset6 {

  public static SBAsset6 load(File file) throws IOException {

    FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
    // Starbound's assets file is about 800mb, so it should be safe to use ByteBuffer, which
    // uses signed ints everywhere.
    if (fileChannel.size() > Integer.MAX_VALUE) {
      throw new AssertionError("File is too large");
    }
    ByteBuffer bytes = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
    
    String magic = readString(bytes, 8);
    if (!magic.equals("SBAsset6")) {
      throw new AssertionError("Not an SBAsset6");
    }

    long metadataOffset = bytes.getLong();
    if (metadataOffset < 0 || metadataOffset > Integer.MAX_VALUE) {
      throw new AssertionError("Metadata address overflows an int");
    }
    
    bytes.position((int)metadataOffset);
    String indexMagic = readString(bytes, 5);
    if (!indexMagic.equals("INDEX")) {
      throw new AssertionError("Invalid index");
    }
    
    @SuppressWarnings("unused")
    Object metadata = Sbon.readMap(bytes);
    long fileCount = Sbon.readVarint(bytes);

    Map<String, Asset> index = new LinkedHashMap<>();
    
    for (long i = 0; i < fileCount; i++) {
      int pathLength = (bytes.get() & 0xFF);
      String path = readString(bytes, pathLength);
      int offset = (int)bytes.getLong();
      int length = (int)bytes.getLong();

      index.put(path, new Asset(offset, length));
    }

    return new SBAsset6(bytes, index);
  }

  private static String readString(ByteBuffer bytes, int length) {
    byte[] stringBytes = new byte[length];
    bytes.get(stringBytes);
    return new String(stringBytes, Charset.forName("utf-8"));
  }
  
  private static class Asset {
    final int offset, length;

    public Asset(int offset, int length) {
      this.offset = offset;
      this.length = length;
    }
  }
  
  private final ByteBuffer bytes;
  private final Map<String, Asset> index;

  public SBAsset6(ByteBuffer bytes, Map<String, Asset> index) {
    this.bytes = bytes;
    this.index = Collections.unmodifiableMap(index);
  }

  public byte[] get(String path) {
    Asset asset = index.get(path);
    if (asset == null) {
      return null;
    }
    
    bytes.position(asset.offset);
    byte[] assetBytes = new byte[asset.length];
    bytes.get(assetBytes);
    return assetBytes;
  }
  
  public Collection<String> getPaths() {
    return index.keySet();
  }
}
