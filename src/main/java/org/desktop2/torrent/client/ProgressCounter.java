package org.desktop2.torrent.client;

public interface ProgressCounter {
  void numberPieces(int numberPieces);

  void downloadPiece(int numberOfPiece);

  void failedToDownloadPiece(int numberOfPiece);
}
