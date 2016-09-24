package starbound.model;

public class WorldId {

  public static WorldId fromId(String id) {
    return new WorldId(id);
  }

  public static WorldId celestialWorldIdFromCoordinates(
      long x, long y, long z, long planet, long satellite) {
    StringBuilder sb = new StringBuilder();
    sb.append("CelestialWorld:");
    sb.append(x).append(":").append(y).append(":").append(z);
    sb.append(":").append(planet);
    if (satellite != 0) {
      sb.append(":").append(satellite);
    }
    return new WorldId(sb.toString());
  }
  
  private final String worldId;
  
  private WorldId(String worldId) {
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
  
  public boolean isInstanceWorldId() {
    return worldId.startsWith("InstanceWorld:");
  }
  
  @Override
  public String toString() {
    return worldId;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WorldId)) {
      return false;
    }
    return worldId.equals(((WorldId)obj).worldId);
  }
  
  @Override
  public int hashCode() {
    return worldId.hashCode();
  }
}
