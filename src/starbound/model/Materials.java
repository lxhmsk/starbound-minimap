package starbound.model;

import java.awt.Color;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import starbound.io.SBAsset6;
import starbound.io.SBAsset6.AssetNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Materials {

  public static Materials create(SBAsset6 assets) {

    List<AssetNode> materialFiles = assets.getDirectory("tiles").findFiles(".material");

    Map<Integer, Material> materials = new HashMap<>();
    JsonParser parser = new JsonParser();
    for (AssetNode matterialFile : materialFiles) {
      byte[] materialBytes = assets.get(matterialFile);
      JsonObject e = parser.parse(new String(materialBytes, Charset.forName("utf-8")))
          .getAsJsonObject();
      int id = e.get("materialId").getAsInt();
      String name = e.get("materialName").getAsString();

      Color color;
      JsonElement particleColor = e.get("particleColor");
      if (particleColor == null) {
        color = null;
      } else {
        JsonArray colorArray = particleColor.getAsJsonArray();
        color = new Color(
            colorArray.get(0).getAsInt(),
            colorArray.get(1).getAsInt(),
            colorArray.get(2).getAsInt());
      }

      materials.put(id, new Material(id, name, color));
    }

    return new Materials(materials);
  }

  public static class Material {
    public final int id;
    public final String name;
    public final Color color;

    public Material(int id, String name, Color color) {
      this.id = id;
      this.name = name;
      this.color = color;
    }
    
    @Override
    public String toString() {
      return "Material " + id + ": " + name;
    }
  }

  private final Map<Integer, Material> materials;

  private Materials(Map<Integer, Material> materials) {
    this.materials = materials;
  }
  
  public Material get(int materialId) {
    return materials.get(materialId);
  }
}
