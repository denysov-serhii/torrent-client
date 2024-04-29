package org.desktop2.torrent.client.peer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.desktop2.torrent.client.RequestBuilder;
import org.desktop2.torrent.client.SocketReader;

@RequiredArgsConstructor
public class DownloadingState implements PeerState {

  private static final MessageDigest digest;

  static {
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private final int index;
  private final byte[] hash;
  private final byte[] storage;

  @Override
  @SneakyThrows
  public void takeControl(Peer context, Socket socket) {
    System.out.println("Downloading...");

    int length = storage.length;

    for (int attempt = 0; attempt < 5; attempt++) {
      int sizeOfData = 2_000; //
      int offset = 0;

      int parts = (length / sizeOfData);
      int left = length - parts * sizeOfData;

      for (int i = 0; i < parts; i++) {
        offset = getOffset(socket, offset, sizeOfData, length);
      }

      if (left > 0) {
        getOffset(socket, offset, left, length);
      }

      byte[] digest1 = digest.digest(storage);
      if (Arrays.equals(digest1, hash)) {
        System.out.println(STR."Hash of piece\{index} is correct");
        context.moveStatusTo(Peer.Status.UN_CHOKE);
        break;
      } else {
        System.out.println(STR."Hash of piece(\{index}) is not correct");
        System.out.println("Try again");
      }
    }
  }

  private int getOffset(Socket socket, int offset, int sizeOfData, int length) throws IOException {
    for (int attepmpt = 0; attepmpt < 10; attepmpt++) {
      socket.getInputStream().skip(socket.getInputStream().available());

      socket.getOutputStream().write(RequestBuilder.request(index, offset, sizeOfData));
      socket.getOutputStream().flush();

      //      byte[] lengthAsBytes = new byte[4];
      //      SocketReader.readBytes(socket, lengthAsBytes, 3);

      int responseLength = SocketReader.readInt(socket, 5);

      if (responseLength <= 0) {

        if (attepmpt != 4) {
          continue;
        }
        System.out.println(STR."Unable to download a piece \{index}");
        throw new RuntimeException("Unable to download a piece");
      }

      int messageId = socket.getInputStream().read();

      if (messageId != 7) {

        if (attepmpt != 4) {
          continue;
        }
        System.out.println("Message is not a piece");
        throw new RuntimeException("Message is not a piece");
      }

      int pieceIndex = SocketReader.readInt(socket, 5);

      if (pieceIndex != index) {
        System.out.println(STR."piece indexes are not equal \{index} <> \{pieceIndex}");
        //                throw new RuntimeException("piece indexes are not equal");
      }

      int blockOffset = SocketReader.readInt(socket, 5);

      int blockSize = responseLength - 9;

      byte[] block = new byte[blockSize];
      int readBytes = SocketReader.readBytes(socket, block, 5);

      System.arraycopy(block, 0, storage, offset, sizeOfData);

      offset += sizeOfData;
      System.out.print(
          STR."Piece # \{
              index} is downloading with progress \{
              round(offset * 1.0d / length, 4) * 100} % \r");

      break;
    }
    return offset;
  }

  public static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();

    BigDecimal bd = BigDecimal.valueOf(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }
}
