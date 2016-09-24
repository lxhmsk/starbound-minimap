package starbound;

import java.util.Map;

import starbound.io.Sbon;
import starbound.io.StarboundFiles;
import starbound.model.Player;
import starbound.model.World;
import starbound.model.WorldId;
import steam.SteamUtils;
import util.TablePrinter;

public class DumpPlayerQuests {

  public static void main(String[] args) throws Exception {

    StarboundFiles starboundFiles = new StarboundFiles(SteamUtils.findStarboundInstallDir());

    Map<WorldId, World> worlds = World.loadWorlds(starboundFiles);

    Player player = Player.loadPlayers(starboundFiles).get(0);

    Sbon quests = player.playerData.getByPath("quests/quests");

    TablePrinter table = new TablePrinter("title", "worldId", "world name");
    table.setSortColumns(2);

    for (Sbon quest : quests.asSbonMap().values()) {
      quest = quest.getByKey("content");
      if (quest.getByKey("state").asString().equals("Active")) {
        String title = quest.getByKey("title").asString();
        String worldId = quest.getByKey("worldId").asString();
        World w = worlds.get(WorldId.fromId(worldId));
        table.addRow(title, worldId, w == null ? "" : w.getName());
      }
    }
    
    table.print();
  }
  
}
