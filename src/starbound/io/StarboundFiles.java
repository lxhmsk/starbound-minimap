package starbound.io;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import steam.SteamUtil;
import steam.VDF;
import steam.VDF.VDFException;
import steam.VDF.VDFNode;

public class StarboundFiles {

  private String starboundInstallDir;
  
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
    String starboundLocation = findStarboundInstallDir();

    File storageSubdir = Paths.get(starboundLocation, "storage", storageSubdirectory).toFile();
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
  
  private String findStarboundInstallDir() {
    
    if (starboundInstallDir != null) {
      return starboundInstallDir;
    }
    
    String steamLocation = SteamUtil.findSteam();
    if (steamLocation == null) {
      return null;
    }

    Path steamConfig = Paths.get(steamLocation, "config", "config.vdf");
    
    VDFNode config;
    try {
      config = VDF.readFile(steamConfig.toString());
    } catch (IOException | VDFException e) {
      System.out.println("Could not read config.vdf: " + e);
      return null;
    }

    VDFNode starboundInstallNode = config.getByPath("Software/Valve/Steam/apps/211820/installdir");
    if (starboundInstallNode == null) {
      System.out.println("Could not find Starbound node in config.vdf");
      return null;
    }
    
    starboundInstallDir = starboundInstallNode.getValue();
    return starboundInstallDir;
  }
  

}
