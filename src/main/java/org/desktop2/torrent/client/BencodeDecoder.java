package org.desktop2.torrent.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BencodeDecoder {

  int start;
  int end;

  @SuppressWarnings("unchecked")
  public Map<String, Object> decode(byte[] bencodeData) throws IOException {
    try (InputStream is = new ByteArrayInputStream(bencodeData)) {
      var map = (Map<String, Object>) readElement(is);

      int indexStart = bencodeData.length - start;
      int size = start - end;

      byte[] hash = new byte[size];
      System.arraycopy(bencodeData, indexStart, hash, 0, size);

      map.put("hashInfo", hash);

      return map;
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

      Object value;
      if (List.of("pieces", "peers").contains(key)) {
        value = readBytesFromBencode(is, is.read());
      } else if (key.equals("info")) {
        start = is.available();
        value = readElement(is);
        end = is.available();
      } else {
        value = readElement(is);
      }

      map.put(key, value);
    }

    System.out.println(map.keySet());
    return map;
  }

  private byte[] readBytesFromBencode(InputStream inputStream, int firstLengthDigit)
      throws IOException {
    // Initialize length with the first digit provided
    int length = firstLengthDigit - '0';
    int characterRead;

    // Read characters until ':' is encountered, calculating the length of the data
    while ((characterRead = inputStream.read()) != ':') {
      if (!Character.isDigit(characterRead)) {
        throw new IOException(
            "Invalid data length in Bencode format: non-digit character encountered.");
      }
      length = length * 10 + (characterRead - '0');
    }

    // Allocate a buffer based on the calculated length and read the data bytes
    byte[] buffer = new byte[length];
    int totalBytesRead = 0;
    int bytesRead;

    // Ensure to read exactly 'length' bytes (handling short reads)
    while (totalBytesRead < length
        && (bytesRead = inputStream.read(buffer, totalBytesRead, length - totalBytesRead)) != -1) {
      totalBytesRead += bytesRead;
    }

    if (totalBytesRead != length) {
      throw new IOException(
          "Invalid data in Bencode format: mismatch in expected and actual length.");
    }

    return buffer;
  }
}
