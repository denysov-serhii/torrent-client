package org.desktop2.torrent.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TorrentMapper {
  private final InfoHashCalculator infoHashCalculator = new InfoHashCalculator();

  @SuppressWarnings("unchecked")
  public Torrent map(Map<String, Object> bencode) {
    System.out.println(bencode.keySet());

    String announce = validateValueType(bencode, "announce", String.class);
    Map<String, Object> info = (Map<String, Object>) validateValueType(bencode, "info", Map.class);
    byte[] infoHash = infoHashCalculator.calculateHash(info);
    var torrentInfo = mapInfoSection(info);

    return new Torrent(
        announce,
        infoHash,
        torrentInfo.name,
        torrentInfo.length,
        torrentInfo.pieceLength,
        torrentInfo.pieces);
  }

  private TorrentInfo mapInfoSection(Map<String, Object> info) {
    long length = validateValueType(info, "length", Long.class);
    String name = validateValueType(info, "name", String.class);
    long pieceLength = validateValueType(info, "piece length", Long.class);
    byte[] piecesAsBytes = validateValueType(info, "pieces", String.class).getBytes();

    List<byte[]> pieces = extractPieces(piecesAsBytes);

    return new TorrentInfo(name, length, pieceLength, pieces);
  }

  @SuppressWarnings("unchecked")
  private <T> T validateValueType(Map<String, ?> map, String key, Class<T> type) {
    if (map.get(key) == null) {
      throw new RuntimeException(STR."The value of '\{key}' key cannot be null");
    }

    if (!type.isAssignableFrom(map.get(key).getClass())) {
      System.out.println(key);
      System.out.println(map.get(key).getClass().getSimpleName());
      throw new RuntimeException(STR."The value of '\{key}' key expected to be \{type.getName()}");
    }

    return (T) map.get(key);
  }

  private static List<byte[]> extractPieces(byte[] piecesAsBytes) {
    List<byte[]> pieces = new ArrayList<>();

    final int SHA_1_LENGTH = 20;

    for (int i = 0; i < piecesAsBytes.length; i += SHA_1_LENGTH) {
      byte[] hash = new byte[SHA_1_LENGTH];
      System.arraycopy(piecesAsBytes, i, hash, 0, SHA_1_LENGTH);
      pieces.add(hash);
    }

    return pieces;
  }

  record TorrentInfo(String name, long length, long pieceLength, List<byte[]> pieces) {}
}
