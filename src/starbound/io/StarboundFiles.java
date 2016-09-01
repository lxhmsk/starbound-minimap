package starbound.io;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarboundFiles {

  private final String starboundInstallDir;
  
  public StarboundFiles(String starboundInstallDir) {
    this.starboundInstallDir = starboundInstallDir;
  }
  
  public List<File> findWorldFiles() {
    return findFiles(".world", "universe");
  }
  
  public List<File> findPlayerFiles() {
    return findFiles(".player", "player");
  }

  public Map<String, File> findClientContextFiles() {
    Map<String, File> map = new HashMap<>();
    List<File> files = findFiles(".clientcontext", "universe");
    for (File file : files) {
      map.put(file.getName().replace(".clientcontext", ""), file);
    }
    return map;
  }

  private List<File> findFiles(String fileExtension, String storageSubdirectory) {

    File storageSubdir = Paths.get(starboundInstallDir, "storage", storageSubdirectory).toFile();
    String[] fleNames = storageSubdir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(fileExtension);
      }
    });

    List<File> files = new ArrayList<>();
    for (String fileName : fleNames) {
      files.add(new File(storageSubdir, fileName));
    }
    return files;
  }
}
