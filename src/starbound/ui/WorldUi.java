package starbound.ui;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import starbound.io.SBAsset6;
import starbound.io.Sbon;
import starbound.io.StarboundFiles;
import starbound.io.VersionedJson;
import starbound.model.Entities;
import starbound.model.Materials;
import starbound.model.Materials.Material;
import starbound.model.Player;
import starbound.model.World;
import starbound.process.StarboundPlayerTracker;
import starbound.process.StarboundPlayerTracker.PlayerTrackingCallback;
import starbound.process.StarboundPlayerTracker.StarboundProcessScannerCallback;
import starbound.ui.ItemSelectionDialog.ItemSelectionListener;
import starbound.ui.ProgressWatcher.ProgressListener;
import starbound.ui.WorldPanel.WorldClickListener;
import starbound.ui.WorldSelectionDialog.WorldSelectionListener;
import util.DirectoryWatcher;
import util.DirectoryWatcher.EventType;
import util.DirectoryWatcher.FileWatchListener;

public class WorldUi {

  private final JFrame frame;
  private final Box controlPanel;
  private final JButton trackPlayerButton, gotoFlagsButton, gotoChestButton, itemsButton,
      reloadButton, resetZoomButton;
  private final JCheckBox centerOnPlayerCheckbox, overlayModeCheckbox, drawWrappedWorld;
  private final WorldPanel worldPanel;
  private final JLabel infoLabel;

  private final StarboundFiles starboundFiles;
  private final WorldDrawer worldDrawer;
  private final Materials materials;

  private List<GameFlag> gameFlags;
  private List<Item> gameItems;
  private List<Chest> ownedChests;
  private World world;

  private final StarboundPlayerTracker playerTracker;
  private final DirectoryWatcher directoryWatcher;
  private final Timer worldUpdateTimer;
  private TimerTask worldUpdateTask;
  
  private boolean inOverlayMode = false;
  private final Dimension previousWindowSize = new Dimension();
  private final Point previousWindowLocation = new Point();

  public WorldUi(StarboundFiles starboundFiles) {

    this.starboundFiles = starboundFiles;

    SBAsset6 assets;
    try {
      assets = SBAsset6.load(starboundFiles.findAssets());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.materials = Materials.create(assets);
    this.worldDrawer = new WorldDrawer(materials);

    playerTracker = new StarboundPlayerTracker();
    try {
      directoryWatcher = new DirectoryWatcher();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    worldUpdateTimer = new Timer("worldUpdateTimer", true);

    frame = new JFrame("Starbound Map");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    worldPanel = new WorldPanel();
    worldPanel.setPreferredSize(new Dimension(1200, 900));
    worldPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3 && inOverlayMode) {
          JPopupMenu menu = new JPopupMenu();
          menu.add(new AbstractAction("Exit Overlay Mode") {
            @Override
            public void actionPerformed(ActionEvent e) {
              
              overlayModeCheckbox.setSelected(false);
              frame.setVisible(false);
              frame.dispose();
              frame.setUndecorated(false);
              frame.setAlwaysOnTop(false);
              controlPanel.setVisible(true);
              
              frame.setLocation(previousWindowLocation);
              frame.setSize(previousWindowSize);

              frame.pack();
              frame.setVisible(true);
              inOverlayMode = false;
            }
          });
          menu.show(worldPanel, e.getX(), e.getY());
        }
      }
    });
    worldPanel.setWorldClickListener(new WorldClickListener() {
      @Override
      public void onClick(int x, int y) {
        int materialId = world.getTileForgroundMaterial().getTile(x, y);
        Material material = materials.get(materialId);
        System.out.println(material);
      }
    });
    frame.add(worldPanel);

    controlPanel = Box.createHorizontalBox();
    infoLabel = new JLabel();
    controlPanel.add(infoLabel);
    controlPanel.add(Box.createHorizontalGlue());

    overlayModeCheckbox = new JCheckBox("Overlay Mode");
    overlayModeCheckbox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        previousWindowSize.setSize(frame.getSize());
        previousWindowLocation.setLocation(frame.getLocation());

        frame.setVisible(false);
        frame.dispose();
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        controlPanel.setVisible(false);
        
        GraphicsDevice gd =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();

        int windowSize = screenWidth / 6;
        frame.setSize(windowSize, windowSize);
        frame.setLocation(screenWidth - windowSize, screenHeight - windowSize);
        
        frame.setVisible(true);
        inOverlayMode = true;
      }
    });
    controlPanel.add(overlayModeCheckbox);
    
    drawWrappedWorld = new JCheckBox("Draw wrapped world");
    drawWrappedWorld.setSelected(worldPanel.getDrawWrappedWorld());
    drawWrappedWorld.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        worldPanel.setDrawWrappedWorld(drawWrappedWorld.isSelected());
      }
    });
    controlPanel.add(drawWrappedWorld);
    
    centerOnPlayerCheckbox = new JCheckBox("Center on Player");
    centerOnPlayerCheckbox.setSelected(true);
    controlPanel.add(centerOnPlayerCheckbox);

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
    
    gotoFlagsButton = new JButton("Flags...");
    gameFlags = new ArrayList<>();
    gotoFlagsButton.addActionListener(new ActionListener() {
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
              centerOnPlayerCheckbox.setSelected(false);
              worldPanel.setViewCenter(gameFlag.x, gameFlag.y);
            }
          }));
        }
        menu.show(gotoFlagsButton, 0, gotoFlagsButton.getHeight());
      }
    });
    controlPanel.add(gotoFlagsButton);

    gotoChestButton = new JButton("Chests...");
    ownedChests = new ArrayList<>();
    gotoChestButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JPopupMenu menu = new JPopupMenu();
        for (Chest chest : ownedChests) {
          String name = chest.type +
              " (" + chest.x + ", " + chest.y + ")";
          JMenu chestMenu = new JMenu(name);
          AbstractAction chestClickAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
              centerOnPlayerCheckbox.setSelected(false);
              worldPanel.setViewCenter(chest.x, chest.y);
            }
          };
          for (Item item : chest.items) {
            JMenuItem itemItem = new JMenuItem(item.toString());
            itemItem.addActionListener(chestClickAction);
            chestMenu.add(itemItem);
          }

          menu.add(chestMenu);
        }
        menu.show(gotoChestButton, 0, gotoChestButton.getHeight());
      }
    });
    controlPanel.add(gotoChestButton);
    
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
              centerOnPlayerCheckbox.setSelected(false);
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
        setWorld(world.file);
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
  
  private void setWorld(File worldFile) {

    boolean enableUi = worldFile != null || this.world != null;
    centerOnPlayerCheckbox.setEnabled(enableUi);
    trackPlayerButton.setEnabled(enableUi);
    gotoFlagsButton.setEnabled(enableUi);
    gotoChestButton.setEnabled(enableUi);
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

        WorldLoadResult results;
        try {
          results = loadWorld(worldFile, starboundFiles, worldDrawer, progressListener);
        } catch (IOException e) {
          System.out.println("Could not load world: " + e);
          return;
        }

        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {

            setWorld(results);
            
            Point start = world.getPlayerStart();
            worldPanel.setViewCenter(start.x, start.y);

            dialog.setVisible(false);
            dialog.dispose();
          }
        });
      }
    });
    thread.start();
  }

  private void setWorld(WorldLoadResult results) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new RuntimeException("Not on UI thread");
    }

    gameFlags = results.gameFlags;
    gameItems = results.gameItems;
    world = results.world;
    ownedChests = results.ownedChests;

    infoLabel.setText(world.getName() + ", " + world.getType() + ", " +
        world.width + " x " + world.height);

    worldPanel.setWorld(results.worldImage);
  }
  
  private static class WorldLoadResult {

    final World world;
    final BufferedImage worldImage;
    final List<GameFlag> gameFlags;
    final List<Item> gameItems;
    final List<Chest> ownedChests;

    public WorldLoadResult(
        World world,
        BufferedImage worldImage,
        List<GameFlag> gameFlags,
        List<Item> gameItems,
        List<Chest> ownedChests) {

      this.world = world;
      this.worldImage = worldImage;
      this.gameFlags = gameFlags;
      this.gameItems = gameItems;
      this.ownedChests = ownedChests;
    }
  }
  
  private static WorldLoadResult loadWorld(
      File worldFile,
      StarboundFiles starboundFiles,
      WorldDrawer worldDrawer,
      ProgressListener progressListener) throws IOException {

    ProgressWatcher watch = new ProgressWatcher(progressListener);

    watch.start("Loading players...");
    List<Player> players = Player.loadPlayers(starboundFiles);

    Map<String, Map<String, String>> playerIdsToBookmarks = new HashMap<>();
    for (Player player : players) {
      playerIdsToBookmarks.put(player.id, player.getBookmarks());
    }
    watch.stop();
    
    watch.start("Loading world...");
    World world = World.load(worldFile);
    watch.stop();

    BufferedImage worldImage = worldDrawer.drawWorld(world, watch);

    List<GameFlag> gameFlags = new ArrayList<>();
    List<Item> gameItems = new ArrayList<>();
    List<Chest> ownedChests = new ArrayList<>();
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

        } else if (Entities.isOwnedChest(entity)) {

          List<Item> items = new ArrayList<>();
          for (Sbon item : entity.data.getByKey("items").asSbonList()) {
            if (item != null) {
              Sbon content = item.getByKey("content");
              items.add(new Item(
                  content.getByKey("name").asString(), content.getByKey("count").asInt(), -1, -1));
            }
          }

          Sbon position = entity.data.getByKey("tilePosition");
          int x = position.getByIndex(0).asInt();
          int y = position.getByIndex(1).asInt();
          ownedChests.add(new Chest(name, items, x, y));
        }

      } else if (entity.identifier.equals("ItemDropEntity")) {

        Sbon content = entity.data.getByPath("item/content");
        String name = content.getByKey("name").asString();
        if (!IgnoredItems.isIgnored(name)) {
          
          int quantity = content.getByKey("count").asInt();
          Sbon position = entity.data.getByKey("position");
          int x = position.getByIndex(0).asInt();
          int y = position.getByIndex(1).asInt();
          
          gameItems.add(new Item(name, quantity, x, y));
        }
      }
    }
    Collections.sort(gameItems);

    return new WorldLoadResult(world, worldImage, gameFlags, gameItems, ownedChests);
  }

  public void selectWorld() {

    List<File> worldFiles = starboundFiles.findWorldFiles();
    if (worldFiles == null) {
      System.out.println("Could not find world files");
      return;
    }

    WorldSelectionDialog worldSelectionDialog;
    try {
      worldSelectionDialog = new WorldSelectionDialog(frame, worldFiles);
    } catch (IOException e) {
      System.out.println("Could not select world: " + e);
      return;
    }

    worldSelectionDialog.setWorldSelectionListener(new WorldSelectionListener() {
      @Override
      public void onWorldSelected(File worldFile) {
        directoryWatcher.clearWatches();

        setWorld(worldFile);

        if (worldFile != null) {
          try {
            directoryWatcher.watchFile(worldFile.toPath(), new FileWatcher());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          
          directoryWatcher.start();
        }
      }
    });

    worldSelectionDialog.show();
  }

  private class FileWatcher implements FileWatchListener {
    @Override
    public void onEvent(Path p, EventType type) {

      if (worldUpdateTask != null) {
        worldUpdateTask.cancel();
      }
      worldUpdateTask = new TimerTask() {
        @Override
        public void run() {

          System.out.println(new Date() + " Reloading " + p);

          WorldLoadResult results;
          try {
            results = loadWorld(world.file, starboundFiles, worldDrawer, null);
          } catch (IOException e) {
            System.out.println("Could not load world: " + e);
            return;
          }
          
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              setWorld(results);
            }
          });
        }
      };

      // Actually do the reload 1 second later because Starbound causes at least 2
      // file update events to be triggered, and also gives it a chance to fully
      // write the file before reading it.
      worldUpdateTimer.schedule(worldUpdateTask, 1000);
    }
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
            worldPanel.setPlayerLocation((int)x, (int)y);
            if (centerOnPlayerCheckbox.isSelected()) {
              worldPanel.setViewCenter((int)x, (int)y);
            }
          }          
        });
      }

      @Override
      public void failure(String msg) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            JOptionPane.showMessageDialog(
                frame, "Failed to start tracking: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
            worldPanel.setTrackingPlayer(false);
            trackPlayerButton.setText("Track Player");
          }          
        });
      }
    });

    dialog.setVisible(true);
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
    
    @Override
    public String toString() {
      if (quantity == 1) {
        return name;
      } else {
        return name + " x" + quantity;
      }
    }
  }

  private static class Chest {
    final String type;
    final List<Item> items;
    final int x, y;
    public Chest(String type, List<Item> items, int x, int y) {
      this.type = type;
      this.items = items;
      this.x = x;
      this.y = y;
    }
  }
}
