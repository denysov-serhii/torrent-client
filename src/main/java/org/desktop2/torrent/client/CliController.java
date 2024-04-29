package org.desktop2.torrent.client;

import static java.lang.StringTemplate.STR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.desktop2.torrent.client.peer.PeerPool;

public class CliController {
  public void handle(String[] args) {
    if (args.length == 0) {
      printAndExit("Need to specify torrent file");
    }

    final var fileName = args[0];
    final var filePath = Paths.get(fileName);

    if (!Files.exists(filePath)) {
      printAndExit(STR."Torrent file with given name(\{fileName}) does not exist");
    }
    byte[] fileContent = null;
    try {
      fileContent = Files.readAllBytes(filePath);
    } catch (IOException e) {
      String message =
          STR."Something went wrong durring reading the file(\{fileName}):\{e.getMessage()}";
      printAndExit(message);
    }

    final var encoder = new BencodeDecoder();

    Map<String, Object> bencode = null;
    try {
      bencode = encoder.decode(fileContent);
    } catch (IOException e) {
      String message =
          STR."Something went wrong durring parsing the file(\{fileName}): \{e.getMessage()}";
      e.printStackTrace();
      printAndExit(message);
    }

    final var torrent = new TorrentMapper().map(bencode);
    final List<Future<?>> futures = new ArrayList<>();

    System.out.println(STR."pieces number: \{torrent.pieces().size()}");

    var peerId = generatePeerID("-MT1000-");
    var peerPool = PeerPool.getInstance(torrent, peerId);
    Map<Integer, byte[]> downloadedPieces = new HashMap<>();
    try (var threadPool = Executors.newCachedThreadPool()) {

      Queue<Integer> omittedPieces = new LinkedList<>();

      for (int pieceIndex = 0; pieceIndex < torrent.pieces().size(); pieceIndex++) {

        var peer = peerPool.take();
        System.out.println("Peer is taken");

        if (!peer.canDownloadPiece(pieceIndex)) {
          omittedPieces.add(pieceIndex);
          System.out.println(STR."This peer cannot give certain piece(\{pieceIndex})");
          continue;
        }

        int savedPieceIndex = pieceIndex;
        futures.add(
            threadPool.submit(
                () -> {
                  downloadedPieces.computeIfAbsent(
                      savedPieceIndex,
                      (_) ->
                          new byte
                              [(int)
                                  torrent.pieceLength()]); // TODO: need to change to piece length
                  peer.downloadPiece(
                      savedPieceIndex,
                      torrent.pieces().get(savedPieceIndex),
                      downloadedPieces.get(savedPieceIndex));
                  peerPool.realise(peer);
                }));
      }

      threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void printAndExit(String message) {
    System.out.println(message);
    System.exit(1);
  }

  private String generatePeerID(String clientPrefix) {
    if (clientPrefix.length() != 8) {
      throw new IllegalArgumentException("Client prefix must be 8 characters long.");
    }

    StringBuilder peerID = new StringBuilder(clientPrefix);
    SecureRandom random = new SecureRandom();

    while (peerID.length() < 20) {
      int nextChar = random.nextInt(10); // Generate a random digit
      peerID.append(nextChar);
    }

    return peerID.toString();
  }
}
