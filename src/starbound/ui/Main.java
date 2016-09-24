package starbound.ui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFileChooser;

import starbound.io.StarboundFiles;
import steam.SteamUtils;
import steam.SteamUtils.SteamException;

public class Main {

  private static final String STARBOUND_DIR_SETTINGS_KEY = "starbound_directory";
  
  public static void main(String[] args) {
    
    String starboundDir = findStarboundDir();
    if (starboundDir == null) {
      return;
    }

    StarboundFiles starboundFiles = new StarboundFiles(starboundDir);

    WorldUi worldUi = new WorldUi(starboundFiles);
    worldUi.setVisible(true);
    worldUi.selectWorld();
  }

  private static String findStarboundDir() {
    
    // 1. Try to find it automatically
    // 2. Try to load it from the settings file
    // 3. Finally, prompt the user
    
    try {
      return SteamUtils.findStarboundInstallDir();
    } catch (SteamException e) {
      
      File settings = new File("settings");
      if (settings.exists()) {
        Properties properties = new Properties();
        try {
          properties.load(new FileReader(settings));
        } catch(IOException ioe) {
          return promptForStarboundDir();
        }

        if (properties.containsKey(STARBOUND_DIR_SETTINGS_KEY)) {
          return properties.getProperty(STARBOUND_DIR_SETTINGS_KEY);
        } else {
          return promptForStarboundDir();
        }
      } else {
        return promptForStarboundDir();
      }
    }
  }

  private static String promptForStarboundDir() {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("Could not auto-locate Starbound: Select Starbound directory");
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int ret = fc.showOpenDialog(null);
    if (ret == JFileChooser.APPROVE_OPTION) {
      String starboundDir = fc.getSelectedFile().getAbsolutePath();
      try(FileWriter fw = new FileWriter("settings")) {
        Properties properties = new Properties();
        properties.setProperty(STARBOUND_DIR_SETTINGS_KEY, starboundDir);
        properties.store(fw, "settings");
      } catch(IOException ioe) {
        ioe.printStackTrace();
        return null;
      }
      return starboundDir;
    } else {
      return null;
    }
  }
}
