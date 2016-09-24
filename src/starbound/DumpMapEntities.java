package starbound;

import java.io.File;

import starbound.io.Sbon;
import starbound.io.VersionedJson;
import starbound.model.World;

public class DumpMapEntities {

  public static void main(String[] args) throws Exception {
    World world = World.load(new File(args[0]));
    
    for (VersionedJson entity : world.getEntities()) {
      if (entity.identifier.equals("ObjectEntity")) {
        if (entity.data.containsKey("items")) {
          boolean hadItem = false;
          for (Sbon item : entity.data.getByKey("items").asSbonList()) {
            if (item != null) {
              hadItem = true;
              int count = item.getByPath("content/count").asInt();
              System.out.print(item.getByPath("content/name"));
              if (count > 1) {
                System.out.print(" x" + count);
              }
              System.out.print(", ");
            }
          }
          if(hadItem) {
            System.out.println();
          }
        }
      }
    }
  }
  
}
