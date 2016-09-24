package starbound.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  
  
  private abstract static class Node {
    final String name;
    
    Node(String name) {
      this.name = name;
    }
  }

  public static class Directory extends Node {
    final Map<String, Node> children;

    public Directory(String name) {
      super(name);
      this.children = new HashMap<>();
    }

    private Directory getOrCreateDirectory(String name) {
      Directory child = (Directory) children.get(name);
      if (child == null) {
        child = new Directory(name);
        children.put(name, child);
      }
      return child;
    }

    /**
     * Returns the subdirectory in this directory of the given name.
     */
    public Directory getDirectory(String name) {
      Node child = children.get(name);
      if (!(child instanceof Directory)) {
        throw new AssertionError(name + " is not a directory");
      }
      return (Directory) child;
    }

    /**
     * Returns all files in this directory that end in the given suffix.
     */
    public List<AssetNode> listFiles(String suffix) {
      List<AssetNode> files = new ArrayList<>();
      for (Node child : children.values()) {
        if (child instanceof AssetNode) {
          AssetNode assetNode = (AssetNode) child;
          if (suffix == null || assetNode.name.endsWith(suffix)) {
            files.add(assetNode);
          }
        }
      }
      return files;
    }

    /**
     * Returns all files in this directory.
     */
    public List<AssetNode> listFiles() {
      return listFiles(null);
    }

    /**
     * Returns all files and directories in this directory.
     */
    public Collection<Node> list() {
      return children.values();
    }

    /**
     * Recursively finds files that end in the given suffix in this directory and its
     * subdirectories.
     */
    public List<AssetNode> findFiles(String suffix) {
      List<AssetNode> files = new ArrayList<>();
      findFiles(suffix, files, this);
      return files;
    }
    
    private static void findFiles(String suffix, List<AssetNode> files, Directory directory) {
      for (Node child : directory.children.values()) {
        if (child instanceof AssetNode) {
          AssetNode assetNodeChild = (AssetNode) child;
          if (suffix == null || assetNodeChild.name.endsWith(suffix)) {
            files.add(assetNodeChild);
          }
        }
      }

      for (Node child : directory.children.values()) {
        if (child instanceof Directory) {
          Directory childDirectory = (Directory) child;
          findFiles(suffix, files, childDirectory);
        }
      }
    }
  }

  public static class AssetNode extends Node {
    
    final Asset asset;
    
    public AssetNode(String name, Asset asset) {
      super(name);
      this.asset = asset;
    }
  }

  private final ByteBuffer bytes;
  private final Map<String, Asset> index;
  private final Directory root;

  private SBAsset6(ByteBuffer bytes, Map<String, Asset> index) {
    this.bytes = bytes;
    this.index = Collections.unmodifiableMap(index);
    

    root = new Directory(null);
    for (String path : index.keySet()) {
      String[] pathParts = path.split("/");
      Directory current = root;
      for (int i = 1; i < pathParts.length - 1; i++) {
        current = current.getOrCreateDirectory(pathParts[i]);
      }
      String assetName = pathParts[pathParts.length - 1];
      current.children.put(assetName, new AssetNode(assetName, index.get(path)));
    }
  }

  public byte[] get(String path) {
    Asset asset = index.get(path);
    if (asset == null) {
      return null;
    }

    return get(asset);
  }
  
  public byte[] get(AssetNode node) {
    return get(node.asset);
  }
  
  private byte[] get(Asset asset) {
    bytes.position(asset.offset);
    byte[] assetBytes = new byte[asset.length];
    bytes.get(assetBytes);
    return assetBytes;
  }

  public Set<String> getPaths() {
    return index.keySet();
  }

  public Directory getDirectory(String path) {
    String[] pathParts = path.split("/");
    Directory current = root;
    for (String part : pathParts) {
      current = current.getDirectory(part);
    }
    return current;
  }
  
  public Directory getRootDirectory() {
    return root;
  }
}
