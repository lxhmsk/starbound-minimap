package starbound.process;

import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import starbound.io.StarboundFiles;
import starbound.model.Player;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

public class StarboundPlayerTracker {

  private static class StarboundProcessScannerException extends Exception {
    public StarboundProcessScannerException(String msg) {
      super(msg);
    }
  }
  
  public static interface User32 extends StdCallLibrary {
       HWND FindWindowExA(HWND hwndParent, HWND childAfter, String className, String windowName);
       HWND FindWindowA(String className, String windowName);
       int GetWindowThreadProcessId(HWND hWnd, IntByReference lpdwProcessId);
  }
  
  public static interface Kernel32 extends StdCallLibrary {
    HANDLE OpenProcess(int fdwAccess, boolean fInherit, int IDProcess);
    boolean ReadProcessMemory(HANDLE process, long address, Pointer buffer, int size, IntByReference numRead);
    void GetSystemInfo(SYSTEM_INFO lpSystemInfo);
    int VirtualQueryEx(HANDLE readprocess, long lpMinimumApplicationAddress, MEMORY_BASIC_INFORMATION lpBuffer, int dwLength);
  }

  public static class MEMORY_BASIC_INFORMATION extends Structure {

    public long baseAddress;
    public long allocationBase;
    public DWORD allocationProtect;
    public long regionSize;
    public DWORD state;
    public DWORD protect;
    public DWORD type;

    @Override
    protected List<?> getFieldOrder() {
      return Arrays.asList(
          "baseAddress",
          "allocationBase",
          "allocationProtect",
          "regionSize",
          "state",
          "protect",
          "type"
      );
    }
  }
  
  private final static Kernel32 kernel32;
  private final static User32 user32;
  
  static {
    Kernel32 k32;
    User32 u32;
    try {
      k32 = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
      u32 = (User32) Native.loadLibrary("user32", User32.class);
    } catch (Exception e) {
      System.out.println("Could not load native libraries: " + e);
      k32 = null;
      u32 = null;
    }
    
    kernel32 = k32;
    user32 = u32;
  }

  public static final int PROCESS_VM_READ = 0x0010;
  public static final int PROCESS_VM_WRITE = 0x0020;
  public static final int PROCESS_VM_OPERATION = 0x0008;
  public static final int PROCESS_QUERY_INFORMATION = 0x0400;

  public static final int MEM_COMMIT = 0x1000;
  public static final int PAGE_PROTECT_READWRITE = 0x04;
  public static final int RET_FAILURE = 0;

  public interface StarboundProcessScannerCallback {
    void searchProgress(int pageCount);
    void searchComplete(boolean success, String failureMsg);
  }
  
  public interface PlayerTrackingCallback {
    void positionUpdate(float x, float y);
    void failure(String msg);
  }

  private boolean running = false;

  public void stop() {
    running = false;
  }

  public boolean running() {
    return running;
  }
  
  public void startStarboundProcessScanner(
      int updateIntervalMillis, StarboundProcessScannerCallback callback,
      PlayerTrackingCallback trackerCallback) {

    if (kernel32 == null || user32 == null) {
      callback.searchComplete(false, "Native libraries not available. Not on windows?");
    }

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {

        HANDLE starboundProcess;
        long positionStructAddress;
        try {
          starboundProcess = findStarboundProcess();
          positionStructAddress = findPositionMemoryAddressFast(starboundProcess, callback);
        } catch (StarboundProcessScannerException e) {
          callback.searchComplete(false, e.getMessage());
          return;
        }
        
        if (positionStructAddress == -1) {
          callback.searchComplete(false, "Could not find position struct");
          return;
        }
        callback.searchComplete(true, "Success");
        System.out.printf("Found position struct at 0x%016X\n", positionStructAddress);

        running = true;
        Memory memory = new Memory(8);
        while (running) {
          boolean success = kernel32.ReadProcessMemory(
              starboundProcess, positionStructAddress, memory, 8, null);
          if (!success) {
            trackerCallback.failure("Could not read process memory. Game terminated?");
            return;
          }
          float x = memory.getFloat(0);
          float y = memory.getFloat(4);
          trackerCallback.positionUpdate(x, y);
          try {
            Thread.sleep(updateIntervalMillis);
          } catch (InterruptedException e) {

          }
        }

        System.out.println("Player tracker thread ending");
      }
    });
    thread.start();
  }
  
  private static HANDLE findStarboundProcess() throws StarboundProcessScannerException {
    int pid = getProcessId("Starbound");
    HANDLE process = openProcess(
        PROCESS_VM_READ | PROCESS_VM_WRITE | PROCESS_VM_OPERATION | PROCESS_QUERY_INFORMATION, pid);
    return process;
  }

  private static long findPositionMemoryAddressFast(
      HANDLE starboundProcess, StarboundProcessScannerCallback callback) throws 
      StarboundProcessScannerException{

    return searchProcessMemory(starboundProcess, callback, new MemoryPageSearcher() {

      private byte[] sentinalBytes = "starbound.log".getBytes(Charset.forName("utf-8"));
      private byte[] bytes = new byte[13];

      @Override
      public int search(Memory page) {

        // There happens to be a string "starbound.log" in the same page as what appears to be
        // the motion controller. The string appears at address 0x06E0 of the page, and the
        // x position, y position, x velocity, y velocity struct appears at 0x0450 of the page.
        page.read(0x06E0, bytes, 0, 13);
        if (Arrays.equals(sentinalBytes, bytes)) {
          return 0x0450;
        }

        return -1;
      }
    });
  }

  @SuppressWarnings("unused")
  private static long findPositionMemoryAddress(
      HANDLE starboundProcess, StarboundProcessScannerCallback callback,
      float x, float y, float vx, float vy) throws StarboundProcessScannerException {

    ByteBuffer bytes = ByteBuffer.allocate(16);
    bytes.order(ByteOrder.LITTLE_ENDIAN);
    bytes.putFloat(x);
    bytes.putFloat(y);
    bytes.putFloat(vx);
    bytes.putFloat(vy);
    
    return searchProcessMemory(starboundProcess, callback, new ByteArraySearcher(bytes.array()));
  }
  
  private interface MemoryPageSearcher {
    int search(Memory page);
  }
  
  private static class ByteArraySearcher implements MemoryPageSearcher {
    private final byte[] bytes;

    ByteArraySearcher(byte[] bytes) {
      this.bytes = bytes;
    }

    @Override
    public int search(Memory page) {
      final long pageSize = page.size();
      page_search:
      for (int i = 0; i < pageSize - bytes.length; i++) {
        for (int b = 0; b < bytes.length; b++) {
          if (page.getByte(i + b) != bytes[b]) {
            continue page_search;
          }
        }
        return i;
      }
      return -1;
    }
  }

  private static long searchProcessMemory(
      HANDLE process, StarboundProcessScannerCallback callback, MemoryPageSearcher searcher) 
      throws StarboundProcessScannerException {

    SYSTEM_INFO info =  new SYSTEM_INFO();
    kernel32.GetSystemInfo(info);
    final long maxAddress = Pointer.nativeValue(info.lpMaximumApplicationAddress);
    final int pageSize = info.dwPageSize.intValue();

    MEMORY_BASIC_INFORMATION mbi = new MEMORY_BASIC_INFORMATION();
    Memory memory = new Memory(pageSize);
    IntByReference numBytesRead = new IntByReference(-1);

    int pageCount = 0;
    long address = Pointer.nativeValue(info.lpMinimumApplicationAddress);
    while (address < maxAddress) {
      int ret = kernel32.VirtualQueryEx(process, address, mbi, mbi.size());
      if (ret != RET_FAILURE) {
        if (mbi.state.intValue() == MEM_COMMIT &&
            mbi.protect.intValue() == PAGE_PROTECT_READWRITE) {

          for (long pageAddress = mbi.baseAddress;
              pageAddress < mbi.baseAddress + mbi.regionSize;
              pageAddress += pageSize) {

            kernel32.ReadProcessMemory(
                process, pageAddress, memory, pageSize, numBytesRead);

            pageCount++;
            if (callback != null) {
              callback.searchProgress(pageCount);
            }

            int i = searcher.search(memory);
            if (i != -1) {
              return pageAddress + i;
            }
          }
          
        }
      }
      if (mbi.regionSize == 0) {
        throw new StarboundProcessScannerException(
            "VirtualQueryEx gave a region size of 0. Not running as administrator?");
      }
      address = mbi.baseAddress + mbi.regionSize;
    }
    return -1;
  }

  private static int getProcessId(String window) throws StarboundProcessScannerException {
    IntByReference pid = new IntByReference(-1);
    HWND hwnd = user32.FindWindowA(null, window);
    if (hwnd == null) {
      throw new StarboundProcessScannerException(
          "Could not find Starbound process. Game not running?");
    }
    user32.GetWindowThreadProcessId(hwnd, pid);
    return pid.getValue();
  }

  private static HANDLE openProcess(int permissions, int pid) {
    HANDLE process = kernel32.OpenProcess(permissions, true, pid);
    return process;
  }

  public static void main(String[] args) throws Exception {

    HANDLE starboundProcess = findStarboundProcess();

    StarboundFiles starboundFiles = new StarboundFiles();

    List<Player> players = Player.loadPlayers(starboundFiles);
    Player player = players.get(0);
    Point2D.Float position = player.getLocationInCurrentWorld();

    System.out.println("Scanning for " + position.x + ", " + position.y);

    //long address = findPositionMemoryAddress(starboundProcess, position.x, position.y, 0, 0);
    long address = findPositionMemoryAddressFast(starboundProcess, null);

    if (address == -1) {
      System.out.println("Position not found...");
      return;
    }
    System.out.printf("Found 0x%016X\n", address);
    
    Memory memory = new Memory(8);
    while (true) {
      boolean r = kernel32.ReadProcessMemory(starboundProcess, address, memory, 8, null);
      float x = memory.getFloat(0);
      float y = memory.getFloat(4);
      System.out.printf("%f, %f, %s\n", x, y, r);
      Thread.sleep(250);
    }
  }
}
