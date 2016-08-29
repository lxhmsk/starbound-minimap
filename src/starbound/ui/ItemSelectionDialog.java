package starbound.ui;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
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

import starbound.ui.WorldFrame.Item;

public class ItemSelectionDialog {

  public interface ItemSelectionListener {
    void onItemSelected(Item item);
  }
  
  private final JDialog dialog;
  private ItemSelectionListener itemSelectionListener;

  public ItemSelectionDialog(JFrame parent, List<Item> items) {
    
    JTable table = createTable(items);

    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          if (itemSelectionListener != null) {
            int index = table.convertRowIndexToModel(table.getSelectedRow());
            Item item = items.get(index);
            ItemSelectionListener tmp = itemSelectionListener;
            // Don't trigger the close listener below
            itemSelectionListener = null;
            dialog.setVisible(false);
            dialog.dispose();
            tmp.onItemSelected(item);
          }
        }
      }
    });

    dialog = new JDialog(parent);
    dialog.setTitle("Select Item");
    dialog.setModalityType(ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.add(new JScrollPane(table));
    
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        if (itemSelectionListener != null) {
          itemSelectionListener.onItemSelected(null);
        }
      }
    });
    
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    
  }
  
  public void setItemSelectionListener(ItemSelectionListener itemSelectionListener) {
    this.itemSelectionListener = itemSelectionListener;
  }

  public void show() {
    dialog.setVisible(true);
  }
  
  private static JTable createTable(List<Item> items) {

    Object[][] tableData = new Object[items.size()][3];
    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      tableData[i][0] = item.name;
      tableData[i][1] = item.quantity;
      tableData[i][2] = "(" + item.x + ", " + item.y + ")";
    }

    JTable table = new JTable(tableData, new Object[] {"Name", "Quantity", "Location"}) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    TableRowSorter<TableModel> tableRowSorter = new TableRowSorter<TableModel>(table.getModel());
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    table.setRowSorter(tableRowSorter);
    table.getRowSorter().setSortKeys(Arrays.asList(new SortKey(0, SortOrder.ASCENDING)));

    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.getColumn("Name").setPreferredWidth(240);
    table.getColumn("Quantity").setPreferredWidth(40);
    table.getColumn("Location").setPreferredWidth(80);
    table.setPreferredScrollableViewportSize(new Dimension((240 + 40 + 80), 600));

    return table;
  }
}
