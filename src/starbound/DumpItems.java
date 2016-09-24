package starbound;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import starbound.io.SBAsset6;
import starbound.io.Sbon;
import starbound.io.StarboundFiles;
import starbound.io.VersionedJson;
import starbound.model.Entities;
import starbound.model.Items;
import starbound.model.Player;
import starbound.model.Weapons;
import starbound.model.Weapons.ValueOrRange;
import starbound.model.Weapons.WeaponConfig;
import starbound.model.World;
import steam.SteamUtils;
import util.TablePrinter;

public class DumpItems {

  public static void main(String[] args) throws Exception {
    
    StarboundFiles starboundFiles = new StarboundFiles(SteamUtils.findStarboundInstallDir());

    List<World> worlds = new ArrayList<>();
    List<File> worldFiles = starboundFiles.findWorldFiles();
    worldFiles.add(starboundFiles.findShipworldFiles().values().iterator().next());
    for (int i = 0; i < worldFiles.size(); i++) {
      World world = World.load(worldFiles.get(i));
      worlds.add(world);
      System.out.printf("% 2d: %s\n", i, world.getName());
    }
    System.out.printf("% 2d: Player Inventory\n", worldFiles.size());

    Scanner scan = new Scanner(System.in);
    int worldIndex = scan.nextInt();
    scan.close();

    List<Sbon> items = new ArrayList<>();
    if (worldIndex == worldFiles.size()) {
      // player
      Player player = Player.loadPlayers(starboundFiles).get(0);
      for (Sbon item : player.getMainInventory()) {
        if (item != null) {
          items.add(item);
        }
      }
    } else {
      World world = worlds.get(worldIndex);
      
      for (VersionedJson entity : world.getEntities()) {
        if (Entities.isOwnedChest(entity)) {
          for (Sbon item : entity.data.getByKey("items").asSbonList()) {
            if (item != null) {
              items.add(item);
            }
          }
        }
      }
    }

    SBAsset6 assets = SBAsset6.load(starboundFiles.findAssets());
    Weapons weapons = Weapons.create(assets);

    TablePrinter table = new TablePrinter(
        "name",
        "quantity",
        "short description",
        "tooltipKind",
        "category",
        "price",
        "level",
        "damage per shot",
        "speed",
        "energy per shot",
        "elementalType",
        "altAbilityType");
    table.setSortColumns(4, -7, -8, 0);
    table.setSeparatingColumn(4);

    for (Sbon item : items) {
      Sbon primaryAbility = item.getByPath("content/parameters/primaryAbility");

      String name =
          item.tryPaths("content/parameters/shortdescription", "content/name").asString();
      int count = item.getByPath("content/count").asInt();

      String weaponType = item.getByPath("content/name").asString();
      WeaponConfig weaponConfig = weapons.getWeaponConfig(weaponType);

      if (weaponConfig == null || primaryAbility == null) {
        table.addRow(name, count, "", "", "", "", "", "", "", "", "", "");
      } else {

        int level = item.getByPath("content/parameters/level").asInt();

        float baseDps =
            getValue(weaponConfig.baseDps, primaryAbility.getByKey("baseDpsFactor"));

        float fireTime =
            getValue(weaponConfig.fireTime, primaryAbility.getByKey("fireTimeFactor"));
        float speed = 1f / fireTime;
        
        float damagePerShot = baseDps * fireTime * Items.getWeaponDamageLevelMultiplier(level);

        float energyUsage =
            getValue(weaponConfig.energyUsage, primaryAbility.getByKey("energyUsageFactor"));
        float energyPerShot = energyUsage * fireTime;
        
        int price = (int)(weaponConfig.price * Items.getItemLevelPriceMultiplier(level));

        String elementType = item.getByPath("content/parameters/elementalType", "").asString();
        String altAbilityType = item.getByPath("content/parameters/altAbilityType", "").asString();

        table.addRow(
            name,
            count,
            weaponConfig.shortDescription,
            weaponConfig.tooltipKind,
            weaponConfig.category,
            price,
            level,
            round(damagePerShot),
            round(speed),
            round(energyPerShot),
            elementType,
            altAbilityType);
      }
    }

    table.print();
  }

  private static String round(float f) {
    return String.format("%4.1f", f);
  }
  
  private static float getValue(ValueOrRange valueOrRange, Sbon factor) {
    if (valueOrRange == null || factor == null) {
      return 0;
    }
    return valueOrRange.getValue(factor.asFloat());
  }
  
}
