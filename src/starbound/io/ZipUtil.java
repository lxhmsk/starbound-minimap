package starbound.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZipUtil {

  public static byte[] decompress(byte[] compressedData) {
    try {
      Inflater decompressor = new Inflater();
      decompressor.setInput(compressedData);
      ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
      byte[] buf = new byte[2048];
      while (!decompressor.finished()) {
        int count = decompressor.inflate(buf);
        bos.write(buf, 0, count);
      }
      bos.close();
      return bos.toByteArray();
    } catch (IOException | DataFormatException e) {
      throw new AssertionError("Could not decompress data", e);
    }
  }
}
