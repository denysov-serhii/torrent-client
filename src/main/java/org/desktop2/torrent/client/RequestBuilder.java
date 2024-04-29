package org.desktop2.torrent.client;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import lombok.SneakyThrows;

public class RequestBuilder {
  private static final int INTERESTED = 2;

  @SneakyThrows
  public static byte[] handshake(byte[] infoHash, String peerId) {
    ByteArrayOutputStream handshake = new ByteArrayOutputStream();
    handshake.write(19);
    handshake.write("BitTorrent protocol".getBytes());
    handshake.write(new byte[8]); // 8 reserved bytes
    handshake.write(infoHash);
    handshake.write(peerId.getBytes());

    return handshake.toByteArray();
  }

  public static byte[] interestedRequest() {
    return new byte[] {0, 0, 0, 1, INTERESTED};
  }

  public static byte[] request(int pieceIndex, int offset, int blockLength) {
    byte[] requestMessage = new byte[17];
    // Length prefix
    requestMessage[0] = 0;
    requestMessage[1] = 0;
    requestMessage[2] = 0;
    requestMessage[3] = 13; // Length of 13 for the payload and message ID

    // Message ID for 'request'
    requestMessage[4] = 6;

    System.arraycopy(ByteBuffer.allocate(4).putInt(pieceIndex).array(), 0, requestMessage, 5, 4);

    System.arraycopy(ByteBuffer.allocate(4).putInt(offset).array(), 0, requestMessage, 9, 4);

    System.arraycopy(ByteBuffer.allocate(4).putInt(blockLength).array(), 0, requestMessage, 13, 4);

    return requestMessage;
  }
}
