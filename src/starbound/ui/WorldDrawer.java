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
import starbound.model.Entities;
import starbound.model.Materials;
import starbound.model.Materials.Material;
import starbound.model.World;
import starbound.model.World.WorldTiles;

public class WorldDrawer {

  private static final Color PLAYER_START_COLOR = new Color(255, 0, 0, 255/2);
  private static final Color PLATFORM_COLOR = new Color(102, 51, 0); // brown
  private static final Color EMPTY_COLOR = new Color(192, 193, 194);
  private static final Color DEFAULT_MATERIAL_COLOR = Color.BLUE;
  private static final Color FLAG_COLOR = Color.CYAN;
  private static final Color CHALLENGE_DOOR_COLOR = Color.MAGENTA;
  private static final Color DROPPED_ITEM_COLOR = Color.YELLOW;
  private static final Color OWNED_CHEST_COLOR = Color.BLUE;
  
  
  private final Materials materials;
  
  public WorldDrawer(Materials materials) {
    this.materials = materials;
  }

  public BufferedImage drawWorld(World world, ProgressWatcher watch) {

    watch.start("Loading tiles...");
    WorldTiles tiles = world.getTileForgroundMaterial();    
    watch.stop();

    // TODO: Reuse an image to reduce memory churn
    BufferedImage image = new BufferedImage(
        tiles.getWidth(), tiles.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D imageGraphics = (Graphics2D) image.getGraphics();
    imageGraphics.setColor(Color.DARK_GRAY);
    imageGraphics.fillRect(0, 0, image.getWidth(), image.getHeight());

    final int torchLightRadius = 12;
    final int surfaceLightPenetration = 8;

    watch.start("Generating world image...");
    for (int x = 0; x < tiles.getWidth(); x++) {
      for (int y = 0; y < tiles.getHeight(); y++) {

        int materialId = tiles.getTile(x, y);
        Color color;
        if (materialId == -2) {
          // Not generated or out of world bounds
          continue;
        } else if (materialId >= 0) {

          Material material = materials.get(materialId);
          if (material == null) {
            System.out.println("no material for " + materialId);
            color = DEFAULT_MATERIAL_COLOR;
          } else if (material.color == null) {
            color = PLATFORM_COLOR;
          } else {
            color = material.color;
          }

        } else {
          // No material
          color = EMPTY_COLOR;
        }

        image.setRGB(x, tiles.getHeight() - y - 1, color.getRGB());
      }
    }
    watch.stop();

    List<VersionedJson> entities = world.getEntities();
    // Objects to draw that should be covered by the torch mask
    for (VersionedJson entity : entities) {
      if (entity.identifier.equals("ObjectEntity")) {
        if (entity.data.getByKey("name").asString().equals("challengedoor")) {
          drawEntity(imageGraphics, tiles, entity, CHALLENGE_DOOR_COLOR);
        }
      }
    }
    
    BufferedImage mask = new BufferedImage(tiles.getWidth(), tiles.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D maskGraphics = (Graphics2D) mask.createGraphics();
    maskGraphics.setColor(Color.BLACK);
    maskGraphics.fillRect(0, 0, mask.getWidth(), mask.getHeight());
    maskGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
    maskGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    maskGraphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
    maskGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    watch.start("Creating torch mask...");
    for (VersionedJson entity : entities) {
      if (entity.identifier.equals("ObjectEntity")) {
        String name =  entity.data.getByKey("name").asString();
        if (name.equals("torch")) {
          Sbon position = entity.data.getByKey("tilePosition");
          int tileX = position.getByIndex(0).asInt();
          int tileY = position.getByIndex(1).asInt();

          int imageX = tileX;
          int imageY = tiles.getHeight() - tileY - 1;

          // draw 3 so that the circles near the edges of the world appear to wrap around to the
          // other side
          drawTrippleCircle(maskGraphics, imageX, imageY, torchLightRadius, tiles.getWidth());
        }
      }
    }
    watch.stop();

    // draw player start rectangle
    Point start = world.getPlayerStart();
    imageGraphics.setColor(PLAYER_START_COLOR);
    imageGraphics.fillRect(
        start.x - 1,
        tiles.getHeight() - start.y - 4,
        3,
        4);

    watch.start("Revealing sky...");
    // reveal the sky and some pixels below the surface
    for (int x = 0; x < tiles.getWidth(); x++) {
      for (int y = tiles.getHeight() - 1; y >= 0; y--) {
        int material = tiles.getTile(x, y);
        if (material >= 0) {
          maskGraphics.drawLine(x, 0, x, tiles.getHeight() - y - 2 + surfaceLightPenetration);
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

        Sbon content = entity.data.getByPath("item/content");
        String name = content.getByKey("name").asString();
        if (!IgnoredItems.isIgnored(name)) {
          drawEntity(imageGraphics, tiles, entity, DROPPED_ITEM_COLOR);
        }

      } else if (entity.identifier.equals("ObjectEntity")) {

        if (entity.data.getByKey("name").asString().contains("flag")) {
          drawEntity(imageGraphics, tiles, entity, FLAG_COLOR);
        } else if (Entities.isOwnedChest(entity)) {
          drawEntity(imageGraphics, tiles, entity, OWNED_CHEST_COLOR);
        }
      }
    }
    watch.stop();

    return image;
  }
  
  private static void drawTrippleCircle(Graphics2D g, int cx, int cy, int r, int w) {
    for (int i = -1; i < 2; i++) {
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
    imageGraphics.fillRect(x - 1, tiles.getHeight() - y - 2, 3, 3);
  }
}
