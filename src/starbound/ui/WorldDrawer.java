package starbound.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import starbound.io.Sbon;
import starbound.io.VersionedJson;
import starbound.model.World;
import starbound.model.World.WorldTiles;

public class WorldDrawer {
  public static BufferedImage drawWorld(World world, ProgressWatcher watch) {

    watch.start("Loading tiles...");
    WorldTiles tiles = world.getTileForgroundMaterial();    
    watch.stop();

    BufferedImage image = new BufferedImage(tiles.getWidth() * 3, tiles.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D imageGraphics = (Graphics2D) image.getGraphics();
    imageGraphics.setColor(Color.DARK_GRAY);
    imageGraphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    
    final int torchLightRadius = 12;
    final int surfaceLightPenetration = 8;

    watch.start("Generating world image...");
    for (int x = 0; x < tiles.getWidth(); x++) {
      for (int y = 0; y < tiles.getHeight(); y++) {

        int material = tiles.getTile(x, y);
        Color color;
        if (material == -2) {
          // Not generated or out of world bounds
          continue;
        } else if (material >= 0) {
          // Some material
          color = Color.BLUE;
        } else {
          // No material
          color = Color.GRAY;
        }

        image.setRGB(x, tiles.getHeight() - y - 1, color.getRGB());
      }
    }
    watch.stop();

    watch.start("Tripplicating world image...");
    imageGraphics.copyArea(0, 0, tiles.getWidth(), tiles.getHeight(), tiles.getWidth(), 0);
    imageGraphics.copyArea(0, 0, tiles.getWidth(), tiles.getHeight(), tiles.getWidth() * 2, 0);
    watch.stop();

    BufferedImage mask = new BufferedImage(tiles.getWidth() * 3, tiles.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D maskGraphics = (Graphics2D) mask.createGraphics();
    maskGraphics.setColor(Color.BLACK);
    maskGraphics.fillRect(0, 0, mask.getWidth(), mask.getHeight());
    maskGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
    maskGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    maskGraphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
    maskGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    watch.start("Creating torch mask...");
    List<VersionedJson> entities = world.getEntities();
    for (VersionedJson entity : entities) {
      if (entity.identifier.equals("ObjectEntity")) {
        String name =  entity.data.getByKey("name").asString();
        if (name.equals("torch")) {
          Sbon position = entity.data.getByKey("tilePosition");
          int tileX = position.getByIndex(0).asInt();
          int tileY = position.getByIndex(1).asInt();

          int imageX = tileX;
          int imageY = tiles.getHeight() - tileY - 1;

          drawTrippleCircle(maskGraphics, imageX, imageY, torchLightRadius, tiles.getWidth());
        }
      }
    }
    watch.stop();

    // draw player start rectangle
    Point start = world.getPlayerStart();
    imageGraphics.setColor(new Color(255, 0, 0, 255/2));
    for (int w = 0; w < 3; w++) {
      imageGraphics.fillRect(
          start.x - 1 + tiles.getWidth() * w,
          tiles.getHeight() - start.y - 4,
          3,
          4);
    }

    watch.start("Revealing sky...");
    // reveal the sky and some pixels below the surface
    for (int x = 0; x < tiles.getWidth(); x++) {
      for (int y = tiles.getHeight() - 1; y >= 0; y--) {
        int material = tiles.getTile(x, y);
        if (material >= 0) {
          for (int w = 0; w < 3; w++) {
            int px = x + w * tiles.getWidth();
            maskGraphics.drawLine(px, 0, px, tiles.getHeight() - y - 2 + surfaceLightPenetration);
          }
          break;
        }
      }
    }
    watch.stop();

    watch.start("Applying torch mask...");
    imageGraphics.drawImage(mask, 0, 0, null);
    watch.stop();

    watch.start("Drawing items...");
    for (VersionedJson entity : entities) {
      if (entity.identifier.equals("ItemDropEntity")) {
        drawEntity(imageGraphics, tiles, entity, Color.YELLOW);
      } else if (entity.identifier.equals("ObjectEntity")) {
        if (entity.data.getByKey("name").asString().contains("flag")) {
          drawEntity(imageGraphics, tiles, entity, Color.CYAN);
        }
      }
    }
    watch.stop();

    return image;
  }
  
  private static void drawTrippleCircle(Graphics2D g, int cx, int cy, int r, int w) {
    for (int i = 0; i < 3; i++) {
      g.fillOval(
          cx - r + w * i,
          cy - r,
          r * 2,
          r * 2);
    }
  }

  private static void drawEntity(
      Graphics2D imageGraphics, WorldTiles tiles, VersionedJson entity, Color color) {

    Sbon position;
    if (entity.data.containsKey("position")) {
      position = entity.data.getByKey("position");
    } else if (entity.data.containsKey("tilePosition")) {
      position = entity.data.getByKey("tilePosition");
    } else {
      throw new IllegalStateException();
    }
    int x = position.getByIndex(0).asInt();
    int y = position.getByIndex(1).asInt();
    imageGraphics.setColor(color);
    for (int w = 0; w < 3; w++) {
      imageGraphics.fillRect(x - 1 + tiles.getWidth() * w, tiles.getHeight() - y - 2, 3, 3);
    }
  }
}
