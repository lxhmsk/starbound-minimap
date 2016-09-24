package util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TablePrinter {

  private final List<String[]> rows;
  private final String[] colNames;

  private int[] sortColumns = null;
  private int separatingColumn = -1;

  public TablePrinter(String ... colNames) {
    this.colNames = colNames;
    rows = new ArrayList<>();
  }

  public void addRow(Object ... entries) {
    if (entries.length != colNames.length) {
      throw new IllegalArgumentException("Number of entries must match columns");
    }

    String[] row = new String[colNames.length];
    for (int i = 0; i < row.length; i++) {
      Object e = entries[i];
      row[i] = e == null ? "null" : e.toString();
    }
    rows.add(row);
  }

  /**
   * Sorts by the first given column, then the second, and so on.
   * <p>If the column index is positive, then that column is sorted ascending.
   * <p>If the column index is negative, then that column is sorted descending.
   */
  public void setSortColumns(int ... columnIndexes) {
    this.sortColumns = Arrays.copyOf(columnIndexes, columnIndexes.length);
  }

  /**
   * Prints a separating line between rows whose values in the given column differ. Only useful
   * if this column is sorted. 
   */
  public void setSeparatingColumn(int separatingColumn) {
    this.separatingColumn = separatingColumn;
  }
  
  public void print() {
    print(System.out);
  }

  public void print(PrintStream out) {
    int[] colWidths = new int[colNames.length];
    for (int i = 0; i < colNames.length; i++) {
      colWidths[i] = getMaxWidth(i);
    }

    List<String[]> sortedRows = rows;
    if (sortColumns != null) {
      sortedRows = new ArrayList<>(rows);
      Collections.sort(sortedRows, new Comparator<String[]>() {
        @Override
        public int compare(String[] o1, String[] o2) {
          for (int sortColumn : sortColumns) {
            int c;
            if (sortColumn < 0) {
              // sort descending
              c = o2[-sortColumn].compareTo(o1[-sortColumn]);
            } else {
              // sort ascending
              c = o1[sortColumn].compareTo(o2[sortColumn]);
            }
            if (c != 0) {
              return c;
            }
          }
          return 0;
        }
      });
    }

    printRowSeparator(out, colWidths, "┌", "┬", "┐");
    printRow(out, colWidths, colNames);
    printRowSeparator(out, colWidths, "├", "┼", "┤");

    for (int i = 0; i < sortedRows.size(); i++) {
      String[] row = sortedRows.get(i);
      printRow(out, colWidths, row);
      if (separatingColumn != -1 && i < sortedRows.size() - 1) {
        String[] nextRow = sortedRows.get(i + 1);
        if (!row[separatingColumn].equals(nextRow[separatingColumn])) {
          printRowSeparator(out, colWidths, "├", "┼", "┤");
        }
      }
    }
    printRowSeparator(out, colWidths, "└", "┴", "┘");
  }

  private static void printRowSeparator(
      PrintStream out, int[] colWidths, String start, String middle, String end) {
    out.print(start);
    for (int i = 0; i < colWidths.length; i++) {
      int w = colWidths[i];
      printChars(out, "─", w + 2);
      if (i < colWidths.length - 1) {
        out.print(middle);
      }
    }
    out.print(end);
    out.println();
  }

  private static void printRow(PrintStream out, int[] colWidths, String[] values) {
    out.print("│");
    for (int i = 0; i < values.length; i++) {
      String value = values[i];
      out.print(" ");
      out.print(value);
      printChars(out, " ", colWidths[i] - value.length());
      out.print(" │");
    }
    out.println();
  }

  private static void printChars(PrintStream out, String s, int count) {
    for (int i = 0; i < count; i++) {
      out.print(s);
    }
  }
  
  private int getMaxWidth(int colIndex) {
    // include the header text
    int max = colNames[colIndex].length();
    for (String[] row : rows) {
      max = Math.max(row[colIndex].length(), max);
    }
    return max;
  }
  
  public static void main(String[] args) {
    TablePrinter tp = new TablePrinter("Foo", "Baaar", "Baz");
    tp.addRow(1, 2, 3);
    tp.addRow(111, 222, 333);
    tp.addRow(1111, 222, 3333);
    tp.addRow(11111111, 222, 333);
    tp.print();
  }
}
