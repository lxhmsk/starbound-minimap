package starbound;

import java.nio.charset.Charset;

import starbound.io.SBAsset6;
import starbound.io.StarboundFiles;
import starbound.io.SBAsset6.AssetNode;
import steam.SteamUtils;
import util.TablePrinter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DumpActiveItemInfo {

  public static void main(String[] args) throws Exception {
    
    StarboundFiles starboundFiles = new StarboundFiles(SteamUtils.findStarboundInstallDir());
    SBAsset6 assets = SBAsset6.load(starboundFiles.findAssets());
    
    TablePrinter t = new TablePrinter("itemName", "tooltipKind", "category");
    t.setSortColumns(1);
    JsonParser parser = new JsonParser();
    for (AssetNode node : assets.getRootDirectory().findFiles(".activeitem")) {
      byte[] weaponActiveItemBytes = assets.get(node);
      JsonObject e = parser.parse(new String(weaponActiveItemBytes, Charset.forName("utf-8")))
          .getAsJsonObject();
      t.addRow(e.get("itemName"), e.get("tooltipKind"), e.get("category"));
    }
    t.print();
  }
}
