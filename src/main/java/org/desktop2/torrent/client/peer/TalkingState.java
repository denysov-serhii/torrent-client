package org.desktop2.torrent.client.peer;

import java.net.Socket;
import java.nio.ByteBuffer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.desktop2.torrent.client.RequestBuilder;
import org.desktop2.torrent.client.SocketReader;

@RequiredArgsConstructor
final class TalkingState implements PeerState {
  @Override
  @SneakyThrows
  public void takeControl(@NonNull Peer context, @NonNull Socket socket) {

    Peer.Endpoint endpoint = context.getEndpoint();
    String name = STR."\{endpoint.ip()}:\{endpoint.port()}";
    System.out.println(STR."Talking with peer(\{name})");
    if (socket.isClosed()) {
      System.out.println("Socket is closed");
      context.moveStatusTo(Peer.Status.FAILED);
    }

    byte[] lengthAsBytes = new byte[4];
    SocketReader.readBytes(socket, lengthAsBytes, 3);

    int length = ByteBuffer.wrap(lengthAsBytes).getInt();

    if (length <= 0) {
      System.out.println(STR."Unable to get info from peer(\{name})");
      context.moveStatusTo(Peer.Status.FAILED);
      return;
    }

    int messageId = socket.getInputStream().read();

    switch (messageId) {
      case 0:
        context.moveStatusTo(Peer.Status.CHOKE);
        break;
      case 1:
        context.moveStatusTo(Peer.Status.UN_CHOKE);
        break;
      case 5:
        System.out.println(STR."Try to get bitField for peer(\{name})");
        byte[] bitField = socket.getInputStream().readNBytes(length - 1);
        System.out.println(STR."bitField is gotten for peer(\{name})");
        context.setBitfield(bitField);

        socket.getOutputStream().write(RequestBuilder.interestedRequest());
        socket.getOutputStream().flush();
        takeControl(context, socket);
        break;
    }
  }
}
