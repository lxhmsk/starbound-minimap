package starbound.ui;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import starbound.io.Sbon;
import starbound.io.StarboundFiles;
import starbound.io.VersionedJson;
import starbound.model.Player;
import starbound.model.World;
import starbound.process.StarboundPlayerTracker;
import starbound.process.StarboundPlayerTracker.PlayerTrackingCallback;
import starbound.process.StarboundPlayerTracker.StarboundProcessScannerCallback;
import starbound.ui.ItemSelectionDialog.ItemSelectionListener;
import starbound.ui.ProgressWatcher.ProgressListener;
import starbound.ui.WorldSelectionDialog.WorldSelectionListener;

public class WorldFrame {

  private final JFrame frame;
  private final JButton trackPlayerButton, gotoButton, itemsButton, reloadButton, resetZoomButton;
  private final WorldPanel worldPanel;
  private final JLabel infoLabel;

  private final StarboundFiles starboundFiles;

  private final List<GameFlag> gameFlags;
  private final List<Item> gameItems;
  private World world;

  private final StarboundPlayerTracker playerTracker;

  public WorldFrame(StarboundFiles starboundFiles) {

    playerTracker = new StarboundPlayerTracker();
    this.starboundFiles = starboundFiles;

    frame = new JFrame("Starbound Map");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    worldPanel = new WorldPanel();
    worldPanel.setPreferredSize(new Dimension(1200, 900));
    frame.add(worldPanel);

    Box controlPanel = Box.createHorizontalBox();
    infoLabel = new JLabel();
    controlPanel.add(infoLabel);
    controlPanel.add(Box.createHorizontalGlue());

    trackPlayerButton = new JButton("Track Player");
    trackPlayerButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (playerTracker.running()) {
          playerTracker.stop();
          trackPlayerButton.setText("Track Player");
          worldPanel.setTrackingPlayer(false);
        } else {          
          trackPlayer();
        }
      }
    });
    controlPanel.add(trackPlayerButton);
    
    gotoButton = new JButton("Go to...");
    gameFlags = new ArrayList<>();
    gotoButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem(new AbstractAction("Start") {
          @Override
          public void actionPerformed(ActionEvent e) {
            Point start = world.getPlayerStart();
            worldPanel.setViewCenter(start.x, start.y);
          }
        }));
        for (GameFlag gameFlag : gameFlags) {
          String name = gameFlag.name +
              " (" + gameFlag.flagType + ") (" + gameFlag.x + ", " + gameFlag.y + ")";
          menu.add(new JMenuItem(new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
              worldPanel.setViewCenter(gameFlag.x, gameFlag.y);
            }
          }));
        }
        menu.show(gotoButton, 0, gotoButton.getHeight());
      }
    });
    controlPanel.add(gotoButton);

    itemsButton = new JButton("Items...");
    gameItems = new ArrayList<>();
    itemsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ItemSelectionDialog dialog = new ItemSelectionDialog(frame, gameItems);
        dialog.setItemSelectionListener(new ItemSelectionListener() {
          @Override
          public void onItemSelected(Item item) {
            if (item != null) {
              worldPanel.setViewCenter(item.x, item.y);
            }
          }
        });
        dialog.show();
      }
    });
    controlPanel.add(itemsButton);
    
    resetZoomButton = new JButton("Reset Zoom");
    resetZoomButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        worldPanel.setZoom(1);
      }
    });
    controlPanel.add(resetZoomButton);
    
    reloadButton = new JButton("Reload");
    reloadButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setWorld(world.file, loadPlayers());
      }
    });
    controlPanel.add(reloadButton);
    
    JButton loadButton = new JButton("Load world");
    loadButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        selectWorld();
      }
    });
    controlPanel.add(loadButton);

    frame.add(controlPanel, BorderLayout.NORTH);

    frame.pack();    
  }

  public void setVisible(boolean visible) {
    frame.setVisible(visible);
  }
  
  private void setWorld(File worldFile, List<Player> players) {

    boolean enableUi = worldFile != null || this.world != null;
    trackPlayerButton.setEnabled(enableUi);
    gotoButton.setEnabled(enableUi);
    itemsButton.setEnabled(enableUi);
    reloadButton.setEnabled(enableUi);
    resetZoomButton.setEnabled(enableUi);

    if (worldFile == null) {
      return;
    }

    JDialog dialog = new JDialog(frame);
    dialog.setPreferredSize(new Dimension(300, 300));
    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    JLabel label = new JLabel("<html>Loading...<br>");
    label.setVerticalAlignment(SwingConstants.TOP);
    dialog.add(label);
    dialog.pack();
    dialog.setLocationRelativeTo(frame);
    dialog.setVisible(true);

    ProgressListener progressListener = new ProgressListener() {
      @Override
      public void starting(String msg) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            label.setText(label.getText() + msg);
          }
        });
      }
      
      @Override
      public void finished(long time) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            label.setText(label.getText() + " (" + time + " ms)<br>");
          }
        });
      }
    };

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {

        ProgressWatcher watch = new ProgressWatcher(progressListener);

        watch.start("Loading world...");
        try {
          world = World.load(worldFile);
        } catch (IOException e) {
          System.out.println("Could not load world: " + e);
          return;
        }
        watch.stop();

        BufferedImage worldImage = WorldDrawer.drawWorld(world, watch);

        Map<String, Map<String, String>> playerIdsToBookmarks = new HashMap<>();
        for (Player player : players) {
          playerIdsToBookmarks.put(player.id, player.getBookmarks());
        }
        
        gameFlags.clear();
        gameItems.clear();
        for (VersionedJson entity : world.getEntities()) {

          if (entity.identifier.equals("ObjectEntity")) {

            String name = entity.data.getByKey("name").asString();
            if (name.contains("flag")) {
              String flagUniqueId = entity.data.getByKey("uniqueId").asString();
              String ownerId = entity.data.getByPath("parameters/owner").asString();
              String flagName = playerIdsToBookmarks.get(ownerId).get(flagUniqueId);
              Sbon position = entity.data.getByKey("tilePosition");
              int x = position.getByIndex(0).asInt();
              int y = position.getByIndex(1).asInt();
              gameFlags.add(new GameFlag(name, flagName, x, y));
            }

          } else if (entity.identifier.equals("ItemDropEntity")) {
            
            Sbon content = entity.data.getByPath("item/content");
            String name = content.getByKey("name").asString();
            int quantity = content.getByKey("count").asInt();
            
            Sbon position = entity.data.getByKey("position");
            int x = position.getByIndex(0).asInt();
            int y = position.getByIndex(1).asInt();
            
            gameItems.add(new Item(name, quantity, x, y));
          }
        }
        Collections.sort(gameItems);

        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            dialog.setVisible(false);
            dialog.dispose();
            
            infoLabel.setText(world.getName() + ", " + world.getType() + ", " +
                world.width + " x " + world.height);

            worldPanel.setWorld(world, worldImage);
          }
        });
      }
    });
    thread.start();
  }

  public void selectWorld() {

    List<Player> players = loadPlayers();
    
    List<File> worldFiles = starboundFiles.findWorldFiles();
    if (worldFiles == null) {
      System.out.println("Could not find world files");
      return;
    }

    WorldSelectionDialog worldSelectionDialog;
    try {
      worldSelectionDialog = new WorldSelectionDialog(frame, worldFiles, players);
    } catch (IOException e) {
      System.out.println("Could not select world: " + e);
      return;
    }

    worldSelectionDialog.setWorldSelectionListener(new WorldSelectionListener() {
      @Override
      public void onWorldSelected(File worldFile) {
        setWorld(worldFile, players);
      }
    });
    
    worldSelectionDialog.show();
  }

  private void trackPlayer() {

    JDialog dialog = new JDialog(frame, "Locating player...");
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.setLocationRelativeTo(frame);
    JLabel progressLabel = new JLabel("Scanned 0 memory pages      ");
    dialog.add(progressLabel);
    dialog.setModalityType(ModalityType.APPLICATION_MODAL);
    dialog.pack();

    playerTracker.startStarboundProcessScanner(32, new StarboundProcessScannerCallback() {
      @Override
      public void searchProgress(int pageCount) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            progressLabel.setText("Scanned " + pageCount + " memory pages");
          }          
        });
      }

      @Override
      public void searchComplete(boolean success, String failureMsg) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (success) {
              worldPanel.setTrackingPlayer(true);
              trackPlayerButton.setText("Stop Tracking Player");
            } else {
              JOptionPane.showMessageDialog(
                  frame, "Failed to start tracking: " + failureMsg, "Error",
                  JOptionPane.ERROR_MESSAGE);
              worldPanel.setTrackingPlayer(false);
            }
            dialog.setVisible(false);
          }          
        });
      }
    },
    new PlayerTrackingCallback() {
      @Override
      public void positionUpdate(float x, float y) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (x < 0 || x > world.width || y < 0 || y > world.height) {
              System.out.println("Tracked position outside of world: " + x + ", " + y);
              return;
            }
            worldPanel.setViewCenter((int)x, (int)y);
          }          
        });
      }

      @Override
      public void failure(String msg) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            worldPanel.setTrackingPlayer(false);
            JOptionPane.showMessageDialog(
                frame, "Failed to start tracking: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
          }          
        });
      }
    });

    dialog.setVisible(true);
  }

  private List<Player> loadPlayers() {
    try {
      return Player.loadPlayers(starboundFiles);
    } catch (IOException e) {
      System.out.println("Could not load players: " + e);
      return null;
    }
  }
  
  private static class GameFlag {

    final String flagType, name;
    final int x, y;

    public GameFlag(String flagType, String name, int x, int y) {
      this.flagType = flagType;
      this.name = name;
      this.x = x;
      this.y = y;
    }
  }
  
  public static class Item implements Comparable<Item> {

    final String name;
    final int quantity;
    final int x, y;

    public Item(String name, int quantity, int x, int y) {
      this.name = name;
      this.quantity = quantity;
      this.x = x;
      this.y = y;
    }
    
    @Override
    public int compareTo(Item o) {
      return name.compareTo(o.name);
    }
  }
}
