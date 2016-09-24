package starbound;

import java.io.File;

import starbound.io.SBAsset6;
import starbound.model.Weapons;
import starbound.model.Weapons.WeaponConfig;

public class DumpWeaponConfigs {

  public static void main(String[] args) throws Exception {

    SBAsset6 assets = SBAsset6.load(new File(args[0]));

    Weapons weapons = Weapons.create(assets);
    
    for (WeaponConfig config : weapons.getWeaponConfigs()) {
      System.out.println(config);
    }
    
  }
  
}