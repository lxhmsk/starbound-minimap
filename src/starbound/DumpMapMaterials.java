package starbound;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import starbound.io.SBAsset6;
import starbound.io.SBAsset6.AssetNode;
import starbound.model.World;
import starbound.model.World.WorldTiles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static java.util.Comparator.comparing;

public class DumpMapMaterials {

  public static void main(String[] args) throws Exception {
    
    World world = World.load(new File(args[0]));
    
    SBAsset6 assets = SBAsset6.load(new File(args[1]));

    Map<Integer, String> materialNames = new HashMap<>();
    JsonParser parser = new JsonParser();
    List<AssetNode> materialFiles = assets.getDirectory("tiles/materials").listFiles(".material");
    for (AssetNode matterialFile : materialFiles) {
      byte[] materialBytes = assets.get(matterialFile);
      JsonObject e = parser.parse(new String(materialBytes, Charset.forName("utf-8"))).getAsJsonObject();
      int id = e.get("materialId").getAsInt();
      String name = e.get("materialName").getAsString();
      materialNames.put(id, name);
    }

    Map<Integer, Integer> count = new HashMap<>();
    WorldTiles tiles = world.getTileForgroundMaterial();
    for (int x = 0; x < tiles.getWidth(); x++) {
      for (int y = 0; y < tiles.getHeight(); y++) {
        int materialId = tiles.getTile(x, y);
        count.put(materialId, count.getOrDefault(materialId, 0) + 1);
      }
    }
    
    List<Entry<Integer, Integer>> entries = new ArrayList<>(count.entrySet());
    entries.sort(comparing(Entry::getValue));
    Collections.reverse(entries);
    
    for (Entry<Integer, Integer> e : entries) {
      System.out.printf("% 5d: % 12d %s\n", e.getKey(), e.getValue(), materialNames.get(e.getKey()));
    }
  }
  
}
