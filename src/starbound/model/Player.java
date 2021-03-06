package starbound.model;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import starbound.io.Sbon;
import starbound.io.StarboundFiles;
import starbound.io.VersionedJson;

public class Player {

  public static List<Player> loadPlayers(StarboundFiles starboundFiles) throws IOException {
    List<Player> players = new ArrayList<>();
    List<File> playerFiles = starboundFiles.findPlayerFiles();
    Map<String, File> clientContextFiles = starboundFiles.findClientContextFiles();
    for (File playerFile : playerFiles) {
      String playerId = playerFile.getName().replace(".player", "");
      try {
        File playerContextFile = clientContextFiles.get(playerId);
        if (playerContextFile == null) {
          throw new AssertionError("Could not find player context file for player " + playerId);
        }
        players.add(Player.load(playerFile, playerContextFile));
      } catch (IOException e) {
        System.out.println("Could not load player file " + playerFile + ": " + e);
        return null;
      }
    }
    return players;
  }
  
  public static Player load(File playerFile, File clientContextFile) throws IOException {
    return new Player(
        VersionedJson.readSbvj01(playerFile).data,
        VersionedJson.readSbvj01(clientContextFile).data);
  }
  
  public final String id;
  public final String name;
  
  public final Sbon playerData;
  private final Sbon clientContext;

  private Player(Sbon playerData, Sbon clientContext) {
    this.playerData = playerData;
    this.clientContext = clientContext;
    this.id = playerData.getByKey("uuid").asString();
    this.name = playerData.getByPath("identity/name").asString();
  }

  /**
   * @return A map of the spawn target (i.e., the unique id) of the flag to the flag's name.
   */
  public Map<String, String> getBookmarks() {
    Map<String, String> bookmarks = new HashMap<>();
    
    Sbon bookmarkData = playerData.getByPath("bookmarks/0/1");
    for (int i = 0; i < bookmarkData.size(); i++) {
      Sbon bookmark = bookmarkData.getByIndex(i);
      bookmarks.put(
          bookmark.getByKey("spawnTarget").asString(),
          bookmark.getByKey("name").asString());
    }

    return bookmarks;
  }

  public WorldId getCurrentWorld() {
    return getCurrentWorld(clientContext);
  }

  public Point2D.Float getSavedLocationInCurrentWorld() {
    return getSavedLocationInCurrentWorld(playerData);
  }
  
  public List<Sbon> getMainInventory() {
    return playerData.getByPath("inventory/mainBag").asSbonList();
  }
  
  public static WorldId getCurrentWorld(Sbon clientContext) {
    Sbon world = clientContext.getByKey("reviveWarp/world");
    return WorldId.fromId(world.asString());
  }

  public static Point2D.Float getSavedLocationInCurrentWorld(Sbon playerData) {
    Sbon position = playerData.getByPath("movementController/position");
    float x = position.getByIndex(0).asFloat();
    float y = position.getByIndex(1).asFloat();
    return new Point2D.Float(x, y);
  }
}
