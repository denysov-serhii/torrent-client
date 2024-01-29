package org.desktop2.torrent.client;

import static java.lang.StringTemplate.STR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

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
      printAndExit(message);
    }

    final var torrent = new TorrentMapper().map(bencode);

    new PeerService().getPeers(torrent);

    System.out.println(STR."announce: \{torrent.announce()}");
    System.out.println(STR."name: \{torrent.name()}");
    System.out.println(STR."length: \{torrent.length()}");
  }

  private static void printAndExit(String message) {
    System.out.println(message);
    System.exit(1);
  }
}
