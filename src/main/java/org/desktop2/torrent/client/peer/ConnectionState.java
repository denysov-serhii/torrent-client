package org.desktop2.torrent.client.peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.desktop2.torrent.client.RequestBuilder;
import org.desktop2.torrent.client.SocketReader;

@RequiredArgsConstructor
final class ConnectionState implements PeerState {
  @Override
  public void takeControl(@NonNull Peer context, @NonNull Socket socket) {

    if (socket.isClosed()) {
      System.out.println("Socket is closed");
      context.moveStatusTo(Peer.Status.DEAD);
    }

    final var endpoint = context.getEndpoint();
    try {
      try {
        socket.connect(new InetSocketAddress(endpoint.ip(), endpoint.port()), 3000);
      } catch (IOException e) {
        //        System.out.println(STR."Unable to connect to \{context.getEndpoint()}");
        context.moveStatusTo(Peer.Status.FAILED);
        return;
      }

      socket
          .getOutputStream()
          .write(RequestBuilder.handshake(context.getInfoHash(), context.getPeerId()));
      socket.getOutputStream().flush();

      byte[] response = new byte[68];
      int readBytes = SocketReader.readBytes(socket, response, 10);

      if (readBytes == 68) {
        System.out.println(STR."Peer(\{context.getEndpoint()}) is connected!!!");
        context.moveStatusTo(Peer.Status.TALKING);
      } else {
        //        System.out.println(STR."Failed with response length: \{readBytes}");
        context.moveStatusTo(Peer.Status.FAILED);
      }
    } catch (IOException e) {
      System.out.println(STR."Something went wrong:\{e.getMessage()}");
      context.moveStatusTo(Peer.Status.FAILED);
    }
  }
}
