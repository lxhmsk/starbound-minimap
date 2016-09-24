package starbound.model;


public class Items {

  public static float getItemLevelPriceMultiplier(int level) {
    if (level > 10) {
      return 5.5f;
    }
    return level * 0.5f + 0.5f;
  }

  public static float getWeaponDamageLevelMultiplier(int level) {
    if (level > 10) {
      return 5.5f;
    }
    return level * 0.5f + 0.5f;
  }

}
