package starbound.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class VersionedJson {

  public final String identifier;
  public final boolean versioned;
  public final int version;
  public final Sbon data;

  private VersionedJson(String identifier, boolean versioned, int version, Sbon data) {
    this.identifier = identifier;
    this.versioned = versioned;
    this.version = version;
    this.data = data;
  }

  public static VersionedJson readSbvj01(File file) throws IOException {

    ByteBuffer bytes = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
    byte[] chars = new byte[6];
    bytes.get(chars);
    String magic = new String(chars, Charset.forName("utf-8"));
    if (!magic.equals("SBVJ01")) {
      throw new AssertionError("File " + file + " is not a SBVJ01 file, magic: " + magic);
    }

    return readVersionedJson(bytes);
  }

  public static VersionedJson readVersionedJson(ByteBuffer bytes) {
    String name = Sbon.readString(bytes);
    boolean versioned = bytes.get() != 0;
    int version = -1;
    if (versioned) {
      version = bytes.getInt();
    }
    Sbon data = Sbon.readSbon(bytes);
    return new VersionedJson(name, versioned, version, data);
  }
}
