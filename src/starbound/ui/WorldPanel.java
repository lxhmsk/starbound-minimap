package starbound.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

class WorldPanel extends JComponent {

  public interface WorldClickListener {
    void onClick(int x, int y);
  }
  
  private BufferedImage worldImage;

  private float zoom = 1f;
  private boolean drawWrappedWorld = true;

  // center of viewport, in tile coordinates
  private int viewX, viewY;
  
  private boolean trackingPlayer;
  // player position, in tile coordinates
  private int playerX, playerY;

  private WorldClickListener worldClickListener;
  
  public WorldPanel() {

    DragListener mouseListener = new DragListener();
    this.addMouseListener(mouseListener);
    this.addMouseMotionListener(mouseListener);
    this.addMouseWheelListener(mouseListener);
  }

  public void setWorld(BufferedImage worldImage) {
    this.worldImage = worldImage;
    repaint();
  }

  public void setWorldClickListener(WorldClickListener worldClickListener) {
    this.worldClickListener = worldClickListener;
  }

  private class DragListener extends MouseAdapter {

    private int tmpViewX, tmpViewY;
    private int mouseDownX, mouseDownY;

    public void mouseClicked(MouseEvent e) {

      int worldX = (viewX + (int)((e.getX() - getWidth() / 2) / zoom)) % worldImage.getWidth();
      int worldY = viewY + (int)((getHeight() / 2 - e.getY()) / zoom);
      
      if (worldX < 0) {
        worldX += worldImage.getWidth();
      }

      if (worldClickListener != null) {
        worldClickListener.onClick(worldX, worldY);
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      tmpViewX = viewX;
      tmpViewY = viewY;
      mouseDownX = e.getX();
      mouseDownY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      int deltaX = e.getX() - mouseDownX;
      int deltaY = e.getY() - mouseDownY;
      viewX = (int) (tmpViewX - deltaX / zoom);
      viewY = (int) (tmpViewY + deltaY / zoom);
      repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      zoom += -e.getWheelRotation() * 0.10;

      if (zoom <= 0.1) {
        zoom = 0.1f;
      }

      repaint();
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    
    if (worldImage == null) {
      return;
    }
    
    int viewportWidth = getWidth();
    int viewportHeight = getHeight();
    int imageWidth = worldImage.getWidth(null);
    int imageHeight = worldImage.getHeight(null);

    int x = -(int) (viewX * zoom - viewportWidth / 2);
    int y = -(int) ((imageHeight - viewY) * zoom - viewportHeight / 2);

    int scaledWidth = (int) (imageWidth * zoom);
    int scaledHeight = (int) (imageHeight * zoom);

    g.drawImage(worldImage, x, y, scaledWidth, scaledHeight, null);
    if (drawWrappedWorld) {
      g.drawImage(worldImage, x - scaledWidth, y, scaledWidth, scaledHeight, null);
      g.drawImage(worldImage, x + scaledWidth, y, scaledWidth, scaledHeight, null);
    }

    if (trackingPlayer) {
      g.setColor(Color.GREEN);
      // player is about 3x4 tiles
      g.fillRect(
          x + (int)((playerX - 1) * zoom),
          y + (int)((imageHeight - (playerY + 2)) * zoom),
          (int)(3 * zoom),
          (int)(4 * zoom));
    }
  }

  public void setViewCenter(int tileX, int tileY) {
    viewX = tileX;
    viewY =  tileY;
    repaint();
  }

  public void setTrackingPlayer(boolean tracking) {
    this.trackingPlayer = tracking;
  }
  
  public void setPlayerLocation(int x, int y) {
    this.playerX = x;
    this.playerY = y;
    repaint();
  }

  public void setZoom(float zoom) {
    this.zoom = zoom;
    repaint();
  }
  
  public void setDrawWrappedWorld(boolean drawWrappedWorld) {
    this.drawWrappedWorld = drawWrappedWorld;
    repaint();
  }
  
  public boolean getDrawWrappedWorld() {
    return drawWrappedWorld;
  }
}