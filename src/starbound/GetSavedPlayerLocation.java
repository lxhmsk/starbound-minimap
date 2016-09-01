package starbound;

import java.awt.geom.Point2D;
import java.io.File;

import starbound.io.Sbon;
import starbound.io.VersionedJson;
import starbound.model.Player;

public class GetSavedPlayerLocation {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("no .player file specified");
      return;
    }
    
    Sbon data = VersionedJson.readSbvj01(new File(args[0])).data;
    Point2D.Float location = Player.getSavedLocationInCurrentWorld(data);
    
    System.out.println(location);
  }
  
}
