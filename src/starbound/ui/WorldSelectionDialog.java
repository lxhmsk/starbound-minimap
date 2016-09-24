package starbound.ui;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import starbound.model.World;

public class WorldSelectionDialog {

  public interface WorldSelectionListener {
    void onWorldSelected(File worldFile);
  }
  
  private final JDialog dialog;
  private WorldSelectionListener worldSelectionListener;

  public WorldSelectionDialog(JFrame parent, List<File> worldFiles)
      throws IOException {
    
    JTable table = createTable(worldFiles);

    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          if (worldSelectionListener != null) {
            int index = table.convertRowIndexToModel(table.getSelectedRow());
            File worldFile = worldFiles.get(index);
            WorldSelectionListener tmp = worldSelectionListener;
            // Don't trigger the close listener below
            worldSelectionListener = null;
            dialog.setVisible(false);
            dialog.dispose();
            tmp.onWorldSelected(worldFile);
          }
        }
      }
    });

    dialog = new JDialog(parent);
    dialog.setTitle("Select World");
    dialog.setModalityType(ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.add(new JScrollPane(table));
    
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        if (worldSelectionListener != null) {
          worldSelectionListener.onWorldSelected(null);
        }
      }
    });
    
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    
  }
  
  public void setWorldSelectionListener(WorldSelectionListener worldSelectionListener) {
    this.worldSelectionListener = worldSelectionListener;
  }

  public void show() {
    dialog.setVisible(true);
  }
  
  private static JTable createTable(List<File> worldFiles) throws IOException {

    Object[][] tableData = new Object[worldFiles.size()][3];
    for (int i = 0; i < worldFiles.size(); i++) {
      File worldFile = worldFiles.get(i);
      World world = World.load(worldFile);
      tableData[i][0] = world.getName();
      tableData[i][1] = world.getType();
      tableData[i][2] = new Date(worldFile.lastModified());
    }

    JTable table = new JTable(tableData, new Object[] {"Name", "Type", "Last Modified"}) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    TableRowSorter<TableModel> tableRowSorter = new TableRowSorter<TableModel>(table.getModel());
    tableRowSorter.setComparator(2, new Comparator<Date>() {
      @Override
      public int compare(Date d1, Date d2) {
        return d1.compareTo(d2);
      }
    });

    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    table.setRowSorter(tableRowSorter);
    table.getRowSorter().setSortKeys(Arrays.asList(new SortKey(2, SortOrder.DESCENDING)));

    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.getColumn("Name").setPreferredWidth(240);
    table.getColumn("Type").setPreferredWidth(80);
    table.getColumn("Last Modified").setPreferredWidth(200);
    table.setPreferredScrollableViewportSize(new Dimension((240 + 80 + 200), 200));

    return table;
  }
}
