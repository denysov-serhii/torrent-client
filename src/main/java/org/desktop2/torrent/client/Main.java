package org.desktop2.torrent.client;

import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.val;

public class Main {
  public static void main(String[] args) {
    if (args.length == 0) {
      printAndExit("Need to specify torrent file");
    }

    final var fileName = args[0];
    final var filePath = Paths.get(fileName);

    val progressCounter = new SimpleProgressCounter();

    new TorrentRunner().handle(fileName, filePath, progressCounter);
  }

  private static void printAndExit(String message) {
    System.out.println(message);
    System.exit(1);
  }

  static class SimpleProgressCounter implements ProgressCounter {

    private final Set<Integer> downloadPieces = ConcurrentHashMap.newKeySet();
    private final Set<Integer> failedPieces = ConcurrentHashMap.newKeySet();
    private int numberPieces;

    @Override
    public void numberPieces(int numberPieces) {
      this.numberPieces = numberPieces;
    }

    @Override
    public void downloadPiece(int numberOfPiece) {
      downloadPieces.add(numberOfPiece);
    }

    @Override
    public void failedToDownloadPiece(int numberOfPiece) {
      failedPieces.add(numberOfPiece);
    }
  }
}
