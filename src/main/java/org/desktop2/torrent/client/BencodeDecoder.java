package org.desktop2.torrent.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BencodeDecoder {

  @SuppressWarnings("unchecked")
  public Map<String, Object> decode(byte[] bencodeData) throws IOException {
    try (InputStream is = new ByteArrayInputStream(bencodeData)) {
      return (Map<String, Object>) readElement(is);
    }
  }

  private Object readElement(InputStream is) throws IOException {
    int code = is.read();
    return switch (code) {
      case 'i' -> readInteger(is);
      case 'l' -> readList(is);
      case 'd' -> readDictionary(is);
      default -> {
        if (Character.isDigit(code)) {
          yield readString(is, code);
        }
        throw new IOException("Invalid Bencode format");
      }
    };
  }

  private Long readInteger(InputStream is) throws IOException {
    StringBuilder sb = new StringBuilder();
    int ch;
    while ((ch = is.read()) != 'e') {
      if (ch == -1) throw new IOException("Invalid integer in Bencode format");
      sb.append((char) ch);
    }
    return Long.parseLong(sb.toString());
  }

  private String readString(InputStream is, int firstDigit) throws IOException {
    int length = firstDigit - '0';
    int ch;
    while ((ch = is.read()) != ':') {
      if (!Character.isDigit(ch)) {
        throw new IOException("Invalid string length in Bencode format");
      }
      length = length * 10 + (ch - '0');
    }
    byte[] buf = new byte[length];
    if (is.read(buf) != length) {
      throw new IOException("Invalid string in Bencode format");
    }
    return new String(buf);
  }

  private List<Object> readList(InputStream is) throws IOException {
    List<Object> list = new ArrayList<>();
    while (true) {
      is.mark(1);
      if (is.read() == 'e') break;
      is.reset();
      list.add(readElement(is));
    }
    return list;
  }

  private Map<String, Object> readDictionary(InputStream is) throws IOException {
    Map<String, Object> map = new LinkedHashMap<>();
    while (true) {
      is.mark(1);
      int code = is.read();
      if (code == 'e') break;
      if (!Character.isDigit(code)) {
        throw new IOException(STR."Invalid dictionary key(\{code}) in Bencode format");
      }

      String key = readString(is, code);
      Object value = readElement(is);
      map.put(key, value);
    }
    return map;
  }
}
