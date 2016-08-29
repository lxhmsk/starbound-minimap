package starbound.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import starbound.model.World;

class WorldPanel extends JComponent {

  private BufferedImage worldImage;
  private int worldWidth;

  private float zoom = 1f;

  // center of viewport, in tile coordinates
  private int viewX, viewY;

  private boolean trackingPlayer;

  public WorldPanel() {

    DragListener mouseListener = new DragListener();
    this.addMouseListener(mouseListener);
    this.addMouseMotionListener(mouseListener);
    this.addMouseWheelListener(mouseListener);
  }

  public void setWorld(World world, BufferedImage worldImage) {
    this.worldImage = worldImage;
    this.worldWidth = world.width;

    Point start = world.getPlayerStart();
    setViewCenter(start.x, start.y);
  }
  
  private class DragListener extends MouseAdapter {

    private int tmpViewX, tmpViewY;
    private int mouseDownX, mouseDownY;

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
      viewY = (int) (tmpViewY - deltaY / zoom);
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
    int y = -(int) (viewY * zoom - viewportHeight / 2);

    g.drawImage(
        worldImage,
        x, y,
        (int) (imageWidth * zoom), (int) (imageHeight * zoom),
        null);
    
    if (trackingPlayer) {
      g.setColor(Color.GREEN);
      // player is about 3x4 tiles
      g.fillRect(
          viewportWidth / 2 - (int)(1 * zoom) ,
          viewportHeight / 2 - (int)(2 * zoom),
          (int)(3 * zoom),
          (int)(4 * zoom));
    }
  }

  public void setViewCenter(int tileX, int tileY) {
    viewX = tileX + worldWidth;
    viewY = worldImage.getHeight(null) - tileY;
    repaint();
  }
  
  public void setTrackingPlayer(boolean tracking) {
    this.trackingPlayer = tracking;
    repaint();
  }
  
  public void setZoom(float zoom) {
    this.zoom = zoom;
    repaint();
  }
}