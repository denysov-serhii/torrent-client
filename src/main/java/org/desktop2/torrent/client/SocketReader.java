package org.desktop2.torrent.client;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;

public class SocketReader {
  @SneakyThrows
  public static int readBytes(Socket socket, byte[] buf, int secondsDelay) {
    var stream = socket.getInputStream();
    for (float i = 0; i < secondsDelay; i += 0.1f) {

      int read = stream.read(buf);

      if (read == 0 || read == -1) {
        TimeUnit.MILLISECONDS.sleep(100);
      } else {
        return read;
      }
    }

    return -1;
  }

  @SneakyThrows
  public static int read(Socket socket, int secondsDelay) {
    var stream = socket.getInputStream();
    for (float i = 0; i < secondsDelay; i += 0.1f) {

      int read = stream.read();

      if (read == -1) {
        TimeUnit.MILLISECONDS.sleep(50);
      } else {
        return read;
      }
    }

    return -1;
  }

  @SneakyThrows
  public static int readInt(Socket socket, int secondsDelay) {
    var stream = socket.getInputStream();
    for (float i = 0; i < secondsDelay; i += 0.1f) {

      if (stream.available() == 0) {
        TimeUnit.MILLISECONDS.sleep(100);
        continue;
      }
      byte[] intBuf = new byte[4];
      int read = stream.read(intBuf);

      if (read == -1) {
        TimeUnit.MILLISECONDS.sleep(100);
      } else {
        return ByteBuffer.wrap(intBuf).getInt();
      }
    }

    return -1;
  }
}
