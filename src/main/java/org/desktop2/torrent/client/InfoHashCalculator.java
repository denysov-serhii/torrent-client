package org.desktop2.torrent.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import lombok.SneakyThrows;

public class InfoHashCalculator {
  @SneakyThrows
  public byte[] calculateHash(Map<String, Object> dencode) {
    var info = encodeDictionary(dencode);
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    return digest.digest(info);
  }

  private byte[] encodeDictionary(Map<String, Object> dictionary) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write('d');

    // TreeMap to ensure keys are sorted
    Map<String, Object> sortedMap = new TreeMap<>(dictionary);

    for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
      out.write(encodeString(entry.getKey()));
      out.write(encodeObject(entry.getValue()));
    }

    out.write('e');
    return out.toByteArray();
  }

  private static byte[] encodeString(String s) {
    return (s.length() + ":" + s).getBytes();
  }

  private byte[] encodeObject(Object obj) throws IOException {
    if (obj instanceof Map) {
      // Recursive call for nested dictionaries
      return encodeDictionary((Map<String, Object>) obj);
    } else if (obj instanceof String) {
      return encodeString((String) obj);
    } else if (obj instanceof Long) {
      return encodeInteger((Long) obj);
    }
    throw new IllegalArgumentException("Unsupported object type");
  }

  private static byte[] encodeInteger(Long i) {
    return ("i" + i + "e").getBytes();
  }
}
