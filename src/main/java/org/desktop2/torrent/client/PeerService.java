package org.desktop2.torrent.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.desktop2.torrent.client.peer.Peer;

public class PeerService {

  private static final int PORT = 6881; // TODO: Need to understand which port should be used

  @SneakyThrows
  public List<Peer> getPeers(Torrent torrent, String peerId) {
    var queryBuilder = new StringBuilder();

    queryBuilder.append(STR."info_hash=\{urlEncode(torrent.infoHash())}&");
    queryBuilder.append(STR."peer_id=\{peerId}&");
    queryBuilder.append(STR."port=\{PORT}&");
    queryBuilder.append(STR."uploaded=0&");
    queryBuilder.append(STR."downloaded=0&");
    queryBuilder.append(STR."compact=1&");
    queryBuilder.append(STR."left=\{torrent.length()}");

    var separator = torrent.announce().contains("?") ? "" : "?";
    String urlString = STR."\{torrent.announce()}\{separator}\{queryBuilder.toString()}";
    System.out.println(STR."URL: \{urlString}");
    var request = HttpRequest.newBuilder().uri(new URI(urlString)).GET().build();
    try (HttpClient client = HttpClient.newHttpClient(); ) {
      HttpResponse<byte[]> response = null;
      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      System.out.println(STR."status: \{response.statusCode()}");

      var rr = new BencodeDecoder().decode(response.body());
      return parsePeers((byte[]) rr.get("peers"), torrent.infoHash(), peerId);
    }
  }

  private String urlEncode(byte[] bytes) {
    StringBuilder encoded = new StringBuilder();
    for (byte b : bytes) {
      encoded.append("%").append(String.format("%02X", b));
    }
    return encoded.toString();
  }

  private List<Peer> parsePeers(byte[] peers, byte[] infoHash, String peerId) {
    // Check if the length of the peers array is a multiple of 6
    if (peers.length % 6 != 0) {
      System.out.println("Invalid peers data.");
      throw new RuntimeException("Invalid peers data.");
    }

    ByteBuffer buffer = ByteBuffer.wrap(peers);

    var result = new ArrayList<Peer>();

    while (buffer.hasRemaining()) {
      try {
        // Read the next 4 bytes for the IP address
        byte[] ipBytes = new byte[4];
        buffer.get(ipBytes);
        InetAddress ip = InetAddress.getByAddress(ipBytes);

        // Read the next 2 bytes for the port number
        int port = buffer.getShort() & 0xFFFF; // Convert to unsigned

        result.add(new Peer(new Peer.Endpoint(ip.getHostAddress(), port), infoHash, peerId));
      } catch (Exception e) {
        throw new RuntimeException("Error parsing peer information: " + e.getMessage());
      }
    }

    return result;
  }
}
