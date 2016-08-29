package starbound.model;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import starbound.io.BTreeDB5;
import starbound.io.Sbon;
import starbound.io.VersionedJson;
import starbound.io.ZipUtil;

public class World {
  
  public static class Region {
    public final int x, y;
    public final Tile[] tiles;
    public final int[] tileForgroundMaterial;
    public final List<VersionedJson> entities;

    public Region(
        int x, int y, Tile[] tiles, int[] tileForegroundMaterial, List<VersionedJson> entities) {
      this.x = x;
      this.y = y;
      this.tiles = tiles;
      this.tileForgroundMaterial = tileForegroundMaterial;
      this.entities = entities;
    }
  }
  
  public static class Tile {

    public final int foreground_material;
    public final int foreground_hue_shift;
    public final int foreground_variant;
    public final int foreground_mod;
    public final int foreground_mod_hue_shift;
    public final int background_material;
    public final int background_hue_shift;
    public final int background_variant;
    public final int background_mod;
    public final int background_mod_hue_shift;
    public final int liquid;
    public final float liquid_level;
    public final float liquid_pressure;
    public final int liquid_infinite;
    public final int collision;
    public final int dungeon_id;
    public final int biome;
    public final int biome_2;
    public final boolean indestructible;

    public Tile(int foreground_material, int foreground_hue_shift, int foreground_variant,
        int foreground_mod, int foreground_mod_hue_shift, int background_material,
        int background_hue_shift, int background_variant, int background_mod,
        int background_mod_hue_shift, int liquid, float liquid_level, float liquid_pressure,
        int liquid_infinite, int collision, int dungeon_id, int biome, int biome_2,
        boolean indestructible) {

      this.foreground_material = foreground_material;
      this.foreground_hue_shift = foreground_hue_shift;
      this.foreground_variant = foreground_variant;
      this.foreground_mod = foreground_mod;
      this.foreground_mod_hue_shift = foreground_mod_hue_shift;
      this.background_material = background_material;
      this.background_hue_shift = background_hue_shift;
      this.background_variant = background_variant;
      this.background_mod = background_mod;
      this.background_mod_hue_shift = background_mod_hue_shift;
      this.liquid = liquid;
      this.liquid_level = liquid_level;
      this.liquid_pressure = liquid_pressure;
      this.liquid_infinite = liquid_infinite;
      this.collision = collision;
      this.dungeon_id = dungeon_id;
      this.biome = biome;
      this.biome_2 = biome_2;
      this.indestructible = indestructible;
    }
  }
  
  public static World load(File file) throws IOException {
    
    BTreeDB5 db = BTreeDB5.load(file);
    
    // 1 byte for layer, 2 bytes for x, 2 bytes for y
    if (db.keySize != 5) {
      throw new AssertionError("World db key size is not 5 bytes");
    }    
    
    // read metadata
    ByteBuffer bytes = get(db, 0, 0, 0);
    if (bytes == null) {
      throw new AssertionError("World has no metadata");
    }
    int width = bytes.getInt();
    int height = bytes.getInt();
    VersionedJson metadata = VersionedJson.readVersionedJson(bytes);
    
    return new World(file, db, width, height, metadata.data);
  }
  
  
  private final BTreeDB5 db;
  
  public final File file;
  public final int width, height;
  public final Sbon metadata;
  
  private List<VersionedJson> cachedEntities;
  
  private World(File file, BTreeDB5 db, int width, int height, Sbon metadata) {
    this.file = file;
    this.db = db;
    this.width = width;
    this.height = height;
    this.metadata = metadata;
  }

  private static final byte[] tempKey = new byte[5];
  private static ByteBuffer get(BTreeDB5 db, int layer, int x, int y) {
    if ((x & 0xFFFF0000) != 0 || (y & 0xFFFF0000) != 0) {
      throw new AssertionError("coords greater than key size: " + x + ", " + y);
    }
    tempKey[0] = (byte) layer;
    tempKey[1] = (byte) ((x >> 8) & 0xFF);
    tempKey[2] = (byte) (x & 0xFF);
    tempKey[3] = (byte) ((y >> 8) & 0xFF);
    tempKey[4] = (byte) (y & 0xFF);
    return get(db, tempKey);
  }
  
  private static ByteBuffer get(BTreeDB5 db, byte[] key) {
    ByteBuffer bytes = db.get(key);
    if (bytes == null) {
      return null;
    }
    return ByteBuffer.wrap(ZipUtil.decompress(bytes.array()));
  }
  
  private ByteBuffer get(int layer, int x, int y) {
    return get(db, layer, x, y);
  }
  
  public Tile[] getTiles(int regionX, int regionY) {
    ByteBuffer bytes = get(1, regionX, regionY);
    if (bytes == null) {
      return null;
    }
    // unknown 3 bytes
    bytes.get();
    bytes.get();
    bytes.get();

    Tile[] tiles = new Tile[32 * 32];

    for (int i = 0; i < 32 * 32; i++) {
      tiles[i] = readTile(bytes);
    }
    return tiles;
  }
  
  public int[] getTileForegroundMaterial(int regionX, int regionY) {
    ByteBuffer bytes = get(1, regionX, regionY);
    if (bytes == null) {
      return null;
    }
    // unknown 3 bytes
    bytes.get();
    bytes.get();
    bytes.get();

    int[] tiles = new int[32 * 32];

    for (int i = 0; i < 32 * 32; i++) {
      tiles[i] = bytes.getShort();
      bytes.position(bytes.position() + 28);
    }
    return tiles;
  }

  public static class WorldTiles {
    private final short[] tiles;
    private final int width, height;

    public WorldTiles(int width, int height, short[] tiles) {
      this.tiles = tiles;
      this.width = width;
      this.height = height;
    }
    
    private void setTile(int x, int y, short v) {
      tiles[y * width + x] = v;
    }
    
    public int getTile(int x, int y) {
      return tiles[y * width + x];
    }
    
    public int getWidth() {
      return width;
    }
    
    public int getHeight() {
      return height;
    }
  }
  
  public WorldTiles getTileForgroundMaterial() {
    short[] tilesArray = new short[width * height];
    WorldTiles tiles = new WorldTiles(width, height, tilesArray);
    Arrays.fill(tilesArray, (short)-2);
    
    List<byte[]> keys = db.getKeys();
    for (byte[] key : keys) {
      if (key[0] == 0x01) {
        int regionX = (key[1] << 8) | key[2];
        int regionY = (key[3] << 8) | key[4];
        ByteBuffer bytes = get(1, regionX, regionY);
        // unknown 3 bytes
        bytes.get();
        bytes.get();
        bytes.get();

        for (int y = 0; y < 32; y++) {
          for (int x = 0; x < 32; x++) {
            short material = bytes.getShort();
            bytes.position(bytes.position() + 28);
            if (material != -2) {
              tiles.setTile(regionX * 32 + x, regionY * 32 + y, material);
            }
          }
        }
      }
    }

    return tiles;
  }
  
  public List<Region> getRegions(boolean fullTiles) {
    List<Region> regions = new ArrayList<>();

    List<byte[]> keys = db.getKeys();
    for (byte[] key : keys) {
      if (key[0] == 0x01) {
        int x = (key[1] << 8) | key[2];
        int y = (key[3] << 8) | key[4];
        Tile[] tiles = null;
        int[] tileForegroundMaterials = null;
        if (fullTiles) {
          tiles = getTiles(x, y);
        } else {
          tileForegroundMaterials = getTileForegroundMaterial(x, y);
        }
        regions.add(new Region(x, y, tiles, tileForegroundMaterials, getEntities(x, y)));
      }
    }

    return regions;
  }
  
  public List<VersionedJson> getEntities(int x, int y) {
    ByteBuffer bytes = get(2, x, y);
    if (bytes == null) {
      return null;
    }
    return readEntities(null, bytes);
  }

  public List<VersionedJson> getEntities() {
    if (cachedEntities != null) {
      return cachedEntities;
    }
    
    List<VersionedJson> entities = new ArrayList<>();
    
    for (byte[] key : db.getKeys()) {
      if (key[0] == 0x02) {
        readEntities(entities, get(db, key));
      }
    }
    
    cachedEntities = Collections.unmodifiableList(entities);
    
    return entities;
  }
  
  private static List<VersionedJson> readEntities(List<VersionedJson> entities, ByteBuffer bytes) {
    int length = (int)Sbon.readVarint(bytes);
    if (entities == null) {
      entities = new ArrayList<>(length);
    }
    for (int i = 0; i < length; i++) {
      entities.add(VersionedJson.readVersionedJson(bytes));
    }
    return entities;
  }

  public Point getPlayerStart() {
    Sbon playerStart = metadata.getByKey("playerStart");
    return new Point(playerStart.getByIndex(0).asInt(), playerStart.getByIndex(1).asInt());
  }

  public String getName() {
    String name = metadata.getByPath(
        "worldTemplate/celestialParameters/name", "Unknown").asString();
    name = name.replaceAll("\\^[^;]*;", "");
    return name;
  }

  public String getType() {
    String type = metadata.getByPath(
        "worldTemplate/celestialParameters/visitableParameters/typeName", "Unknown").asString();
    return type.substring(0, 1).toUpperCase() + type.substring(1);
  }
  
  //private static final byte[] tileBuffer = new byte[30];
  private static Tile readTile(ByteBuffer bytes) {

    //bytes.get(tileBuffer);
    
    int foreground_material = bytes.getShort();
    int foreground_hue_shift = (bytes.get() & 0xFF);
    int foreground_variant = (bytes.get() & 0xFF);
    int foreground_mod = bytes.getShort();
    int foreground_mod_hue_shift = (bytes.get() & 0xFF);
    int background_material = bytes.getShort();
    int background_hue_shift = (bytes.get() & 0xFF);
    int background_variant = (bytes.get() & 0xFF);
    int background_mod = bytes.getShort();
    int background_mod_hue_shift = (bytes.get() & 0xFF);
    int liquid = (bytes.get() & 0xFF);
    float liquid_level = bytes.getFloat();
    float liquid_pressure = bytes.getFloat();
    int liquid_infinite = (bytes.get() & 0xFF);
    int collision = (bytes.get() & 0xFF);
    int dungeon_id = (bytes.getShort() & 0xFFFF);
    int biome = (bytes.get() & 0xFF);
    int biome_2 = (bytes.get() & 0xFF);
    boolean indestructible = bytes.get() > 0;
    
    return new Tile(
        foreground_material,
        foreground_hue_shift,
        foreground_variant,
        foreground_mod,
        foreground_mod_hue_shift,
        background_material,
        background_hue_shift,
        background_variant,
        background_mod,
        background_mod_hue_shift,
        liquid,
        liquid_level,
        liquid_pressure,
        liquid_infinite,
        collision,
        dungeon_id,
        biome,
        biome_2,
        indestructible);
  }
}
