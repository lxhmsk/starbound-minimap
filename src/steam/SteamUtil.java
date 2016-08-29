package steam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SteamUtil {

  private static final String queryCmdLn =
      "reg query HKEY_CURRENT_USER\\Software\\Valve\\Steam /v SteamPath";
  
  public static String findSteam() {
    String regOutput;
    try {
      Process reg = Runtime.getRuntime().exec(queryCmdLn);
      regOutput = getString(reg.getInputStream());
    } catch (IOException e) {
      return null;
    }

    String[] regLines = regOutput.split("\\n");
    String[] regCols = regLines[2].split("[ ]+", 4);
    return regCols[3].trim();
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
 
  
  public static void main(String[] args) {
    System.out.println("Steam: " + SteamUtil.findSteam());
  }
}
