package starbound.model;

public class WorldId {

  private final String worldId;
  
  public WorldId(String worldId) {
    this.worldId = worldId;
  }
  
  public String toFileName() {
    if (isCelestialWorldId()) {
      return worldId.substring("CelestialWorld:".length()).replace(":", "_") + ".world";
    } else if (isClientShipWorldId()) {
      return worldId.substring("ClientShipWorld:".length()) + ".shipworld";
    } else {
      return null;
    }
  }
  
  public boolean isCelestialWorldId() {
    return worldId.startsWith("CelestialWorld");
  }
  
  public boolean isClientShipWorldId() {
    return worldId.startsWith("ClientShipWorld");
  }
}
