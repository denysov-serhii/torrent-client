package org.desktop2.torrent.client;

import java.util.List;

public record Torrent(
    String announce,
    byte[] infoHash,
    String name,
    long length,
    long pieceLength,
    List<byte[]> pieces) {}
