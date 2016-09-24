package starbound.model;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import starbound.io.SBAsset6;
import starbound.io.SBAsset6.AssetNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Weapons {

  public static Weapons create(SBAsset6 assets) {

    List<AssetNode> weaponActiveItems =
        assets.getDirectory("items/active/weapons").findFiles(".activeitem");
    
    Map<String, WeaponConfig> weaponConfigs = new HashMap<>();
    JsonParser parser = new JsonParser();

    for (AssetNode weaponActiveItem : weaponActiveItems) {

      byte[] weaponActiveItemBytes = assets.get(weaponActiveItem);
      JsonObject e = parser.parse(new String(weaponActiveItemBytes, Charset.forName("utf-8")))
          .getAsJsonObject();

      JsonElement primaryAbilityElement = e.get("primaryAbility");
      if (primaryAbilityElement != null) {

        JsonObject primaryAbility = primaryAbilityElement.getAsJsonObject();
        String name = e.get("itemName").getAsString();
        String shortdescription = e.get("shortdescription").getAsString();

        String tooltipKind;
        JsonElement tooltipKindElement = e.get("tooltipKind");
        if (tooltipKindElement == null) {
          tooltipKind = "";
        } else {
          tooltipKind = tooltipKindElement.getAsString();
        }
        String category = e.get("category").getAsString();
          
        int price = e.get("price").getAsInt();

        WeaponConfig weaponConfig = new WeaponConfig(
            name,
            shortdescription,
            tooltipKind,
            category,
            price,
            getValueOrRange(primaryAbility.get("fireTime")),
            getValueOrRange(primaryAbility.get("baseDps")),
            getValueOrRange(primaryAbility.get("energyUsage")));
        weaponConfigs.put(name, weaponConfig);
      }
    }
    
    return new Weapons(weaponConfigs);
  }
  
  private static ValueOrRange getValueOrRange(JsonElement e) {
    if (e == null) {
      return ValueOrRange.ZERO;
    } else if (e.isJsonPrimitive()) {
      return new ValueOrRange(e.getAsFloat());
    } else if (e.isJsonArray()) {
      JsonArray values = e.getAsJsonArray();
      return new ValueOrRange(values.get(0).getAsFloat(), values.get(1).getAsFloat());
    } else {
      throw new AssertionError("Don't know how to interpret this weapon price: " + e);
    }
  }
  
  public static class ValueOrRange {
    public static final ValueOrRange ZERO = new ValueOrRange(0); 
    
    private final float value, min, max;
    public final boolean isValue;

    private ValueOrRange(float value) {
      this.value = value;
      this.min = Float.NaN;
      this.max = Float.NaN;
      this.isValue = true;
    }

    private ValueOrRange(float min, float max) {
      this.value = Float.NaN;
      this.min = min;
      this.max = max;
      this.isValue = false;
    }
    
    public float getValue(float factor) {
      if (isValue) {
        return value;
      } else {
        return min + (max - min) * factor;
      }
    }
    
    @Override
    public String toString() {
      if (isValue) {
        return String.valueOf(value);
      } else {
        return String.format("[%f, %f]", min, max); 
      }
    }
  }
  
  public static class WeaponConfig {
    
    public final String name;
    public final String shortDescription;
    public final String tooltipKind;
    public final String category;
    public final int price;

    public final ValueOrRange fireTime;
    public final ValueOrRange baseDps;
    public final ValueOrRange energyUsage;

    public WeaponConfig(
        String name,
        String shortDescription,
        String tooltipKind,
        String category,
        int price,
        ValueOrRange fireTime,
        ValueOrRange baseDps,
        ValueOrRange energyUsage) {

      this.name = name;
      this.shortDescription = shortDescription;
      this.tooltipKind = tooltipKind;
      this.category = category;
      this.price = price;
      this.fireTime = fireTime;
      this.baseDps = baseDps;
      this.energyUsage = energyUsage;
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("WeaponConfig ").append(name).append(" ");
      sb.append("fireTime: ").append(fireTime).append(", ");
      sb.append("baseDps: ").append(baseDps).append(", ");
      sb.append("energyUsage: ").append(energyUsage);
      return sb.toString();
    }
  }

  private final Map<String, WeaponConfig> weaponConfigs;

  public Weapons(Map<String, WeaponConfig> weaponConfigs) {
    this.weaponConfigs = weaponConfigs;
  }
  
  public WeaponConfig getWeaponConfig(String name) {
    return weaponConfigs.get(name);
  }
  
  public Collection<WeaponConfig> getWeaponConfigs() {
    return weaponConfigs.values();
  }
}
