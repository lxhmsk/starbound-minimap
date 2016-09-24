package starbound.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IgnoredItems {

  public static final Set<String> ITEMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      "cobblestonematerial",
      "greenslime",
      "dirtmaterial",
      "wetdirt",
      "slime",
      "limestone",
      "mud",
      "tar",
      "sand",
      "sand2",
      "snow",
      "ice",
      "rockbrickmaterial",
      "copperblock"
  )));
  
  public static boolean isIgnored(String name) {
    return ITEMS.contains(name);
  }
  
}
