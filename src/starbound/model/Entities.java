package starbound.model;

import starbound.io.VersionedJson;

public class Entities {

  public static boolean isOwnedChest(VersionedJson entity) {
    return 
        entity.identifier.equals("ObjectEntity") &&
        entity.data.containsKey("items") &&
        (entity.data.getByPath("parameters/owner") != null ||
        entity.data.getByKey("name").asString().contains("shiplocker"));
  }

}
