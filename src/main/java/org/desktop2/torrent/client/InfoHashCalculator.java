package org.desktop2.torrent.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import lombok.SneakyThrows;

public class InfoHashCalculator {
  @SneakyThrows
  public byte[] calculateHash(Map<String, Object> dencode, byte[] infoHash) {
    System.out.println("calculateHash call:");
    var info = encodeDictionary(dencode);
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    var hash = digest.digest(info);

    System.out.println("Should be: 8DD241F1 43DC3470 BCCA96F2 95B47FEA 054A2F69");
    System.out.println(STR."is: \t\{toHexString(hash)}");

    var hash2 = digest.digest(infoHash);

    System.out.println(STR."maybe should be so: \t\{toHexString(hash2)}");
    return hash2;
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
    if (obj instanceof byte[] bytes) {
      return bytes;
    } else if (obj instanceof Map) {
      // Recursive call for nested dictionaries
      return encodeDictionary((Map<String, Object>) obj);
    } else if (obj instanceof String str) {
      return encodeString(str);
    } else if (obj instanceof Long number) {
      return encodeInteger(number);
    }
    throw new IllegalArgumentException("Unsupported object type");
  }

  private static byte[] encodeInteger(Long i) {
    return ("i" + i + "e").getBytes();
  }

  private String toHexString(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      String hex = Integer.toHexString(0xFF & bytes[i]);
      if (hex.length() == 1) {
        // Append a leading zero if the hex length is 1 (to ensure 2 digits per byte)
        hexString.append('0');
      }
      hexString.append(hex.toUpperCase());
      if ((i + 1) % 4 == 0 && (i + 1) < bytes.length) {
        // Insert a space every 4 bytes, but not at the end
        hexString.append(" ");
      }
    }
    return hexString.toString();
  }
}
