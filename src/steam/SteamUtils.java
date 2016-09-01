package steam;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import steam.VDF.VDFException;
import steam.VDF.VDFNode;

public class SteamUtils {

  public static class SteamException extends Exception {
    private SteamException(String msg, Throwable cause) {
      super(msg, cause);
    }
    
    private SteamException(String msg) {
      super(msg);
    }
  }

  private static final String queryCmdLn =
      "reg query HKEY_CURRENT_USER\\Software\\Valve\\Steam /v SteamPath";
  
  public static String findSteam() throws SteamException {
    String regOutput;
    try {
      Process reg = Runtime.getRuntime().exec(queryCmdLn);
      regOutput = getString(reg.getInputStream());
    } catch (IOException e) {
      throw new SteamException("Could not find steam", e);
    }

    String[] regLines = regOutput.split("\\n");
    String[] regCols = regLines[2].split("[ ]+", 4);
    String f = regCols[3].trim();
    if (!new File(f).exists()) {
      throw new SteamException("Queried for steam location but path does not exist: " + f);
    }
    return f;
  }

  private static String getString(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
        result.write(buffer, 0, length);
    }
    return result.toString("UTF-8");
  }
 
  public static String findStarboundInstallDir() throws SteamException {
    
    String steamLocation = SteamUtils.findSteam();

    Path steamConfig = Paths.get(steamLocation, "config", "config.vdf");
    
    VDFNode config;
    try {
      config = VDF.readFile(steamConfig.toString());
    } catch (IOException | VDFException e) {
      throw new SteamException("Could not read config.vdf", e);
    }

    VDFNode starboundInstallNode = config.getByPath("Software/Valve/Steam/apps/211820/installdir");
    if (starboundInstallNode == null) {
      throw new SteamException("Could not find Starbound node in config.vdf");
    }
    return starboundInstallNode.getValue();
  }

  public static void main(String[] args) throws Exception {
    System.out.println("Steam: " + SteamUtils.findSteam());
  }
}
