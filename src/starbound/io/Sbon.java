package starbound.io;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Sbon {

  private final Object value;
  
  private Sbon(Object value) {
    this.value = value;
  }

  public Sbon getByPath(String path) {
    String[] keys = path.split("/");
    Sbon current = this;
    for (String key : keys) {
      if (current == null) {
        return null;
      }
      if (current.isMap()) { 
        current = current.getByKey(key);
      } else if (current.isList()) {
        int index = Integer.parseInt(key);
        current = current.getByIndex(index);
      } else {
        throw new AssertionError("Cannot traverse to key " + key + " in path " + path +
            " because current element is not a list or a map: " + value);
      }
    }
    return current;
  }

  public Sbon getByPath(String path, Object defaultValue) {
    Sbon value = getByPath(path);
    if (value == null) {
      return new Sbon(defaultValue);
    }
    return value;
  }
  
  public Sbon getByKey(String key) {
    return new Sbon(((Map<?, ?>) value).get(key));
  }
  
  public Sbon getByIndex(int index) {
    List<?> l = (List<?>) value;
    return new Sbon(l.get(index));
  }

  public int size() {
    return ((Collection<?>) value).size();
  }
  
  public boolean containsKey(String key) {
    return ((Map<?, ?>) value).containsKey(key);
  }
  
  public boolean isList() {
    return value instanceof List;
  }
  
  public boolean isMap() {
    return value instanceof Map;
  }
  
  public int asInt() {
    return ((Number) value).intValue();
  }
  
  public double asDouble() {
    return ((Number) value).doubleValue();
  }
  
  public float asFloat() {
    return ((Number) value).floatValue();
  }


  public long asLong() {
    return ((Number) value).longValue();
  }
  
  public String asString() {
    return (String) value;
  }
  
  public boolean asBoolean() {
    return ((Boolean) value).booleanValue();
  }
  
  @SuppressWarnings("unchecked")
  public Map<String, ?> asMap() {
    return (Map<String, ?>) value;
  }

  public List<?> asList() {
    return (List<?>) value;
  }
  
  public String toString() {
    return value == null ? "null" : value.toString();
  }
  
  public static String readString(ByteBuffer bytes) {
    int length = (int)readVarint(bytes);
    byte[] chars = new byte[length];
    bytes.get(chars);
    return new String(chars, Charset.forName("utf-8"));
  }
  
  public static long readVarint(ByteBuffer bytes) {
    long value = 0;
    while (true) {
      byte b = bytes.get();
      if ((b & 0b1000_0000) == 0) {
        return value << 7 | b;
      }
      value = value << 7 | (b & 0b0111_1111);
    }
  }

  public static Sbon readSbon(ByteBuffer bytes) {
    return new Sbon(readDynamic(bytes));
  }
  
  public static Object readDynamic(ByteBuffer bytes) {
    byte type = bytes.get();
    switch (type) {
    case 1:
      return null;
    case 2:
      return bytes.getDouble();
    case 3:
      return bytes.get() != 0;
    case 4:
      return readSignedVarint(bytes);
    case 5:
      return readString(bytes);
    case 6:
      return readList(bytes);
    case 7:
      return readMap(bytes);
    default:
      throw new AssertionError("Unknown dynamic type: " + type);
    }
  }
  
  private static long readSignedVarint(ByteBuffer bytes) {
    long v = readVarint(bytes);
    if ((v & 1) != 0) {
      return -(v >> 1);
    } else {
      return v >> 1;
    }
  }
  
  private static List<Object> readList(ByteBuffer bytes) {
    int length = (int)readVarint(bytes);
    ArrayList<Object> list = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      list.add(readDynamic(bytes));
    }
    return list;
  }
  
  public static Map<String, ?> readMap(ByteBuffer bytes) {
    int length = (int)readVarint(bytes);
    Map<String, Object> map = new HashMap<>(length);
    for (int i = 0; i < length; i++) {
      String key = readString(bytes);
      Object value = readDynamic(bytes);
      map.put(key, value);
    }
    return map;
  }
  
  public void debugPrint() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    System.out.println(gson.toJson(value));
  }
}
