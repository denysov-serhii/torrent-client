package org.desktop2.torrent.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.List;
import lombok.SneakyThrows;

public class PeerService {

  private static final int PORT = 443; // TODO: Need to understand which port should be used

  @SneakyThrows
  public List<Peer> getPeers(Torrent torrent) {
    var queryBuilder = new StringBuilder();

    queryBuilder.append(STR."info_hash=\{urlEncode(torrent.infoHash())}&");
    queryBuilder.append(STR."peer_id=\{generatePeerID("-MT1000-")}&");
    //    queryBuilder.append(STR."port=\{PORT}&");
    queryBuilder.append(STR."uploaded=0&");
    queryBuilder.append(STR."downloaded=0&");
    queryBuilder.append(STR."compact=1&");
    queryBuilder.append(STR."left=\{torrent.length()}");

    var request =
        HttpRequest.newBuilder()
            .uri(new URI(STR."\{torrent.announce()}?\{queryBuilder.toString()}"))
            .GET()
            .build();
    try (HttpClient client = HttpClient.newHttpClient(); ) {
      HttpResponse<String> response = null;
      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      System.out.println(response.body());
    }

    return List.of();
  }

  public static String generatePeerID(String clientPrefix) {
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

  private static String urlEncode(byte[] bytes) {
    StringBuilder encoded = new StringBuilder();
    for (byte b : bytes) {
      encoded.append("%").append(String.format("%02X", b));
    }
    return encoded.toString();
  }
}
