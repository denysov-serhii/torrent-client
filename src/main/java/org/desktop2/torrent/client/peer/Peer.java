package org.desktop2.torrent.client.peer;

import java.net.Socket;
import lombok.*;

@RequiredArgsConstructor
public class Peer {
  @Getter private final Endpoint endpoint;
  @Getter private final byte[] infoHash;
  @Getter private final String peerId;
  @Getter @Setter private byte[] bitfield;
  private final Socket socket = new Socket();

  @Getter private volatile Status status = Status.NEW;
  private PeerState state = new NothingToDoState("NEW");

  @SneakyThrows
  public void downloadPiece(int pieceIndex, byte[] pieceHash, byte[] storage) {
    status = Status.DOWNLOADING;
    state = new DownloadingState(pieceIndex, pieceHash, storage);

    state.takeControl(this, socket);
  }

  public void moveStatusTo(Status status) {
    String name = STR."\{endpoint.ip}:\{endpoint.port}";
    System.out.println(STR."Try to move status to \{status} for peer(\{name})");

    this.status = status;

    this.state =
        switch (status) {
          case NEW -> new NothingToDoState("NEW");
          case CONNECTING -> new ConnectionState();
          case TALKING -> new TalkingState();
          case CHOKE -> new NothingToDoState("CHOKE");
          case UN_CHOKE -> new NothingToDoState("UN_CHOKE");
          case DOWNLOADING ->
              throw new RuntimeException("Cannot move peer to status downloading directly");
          case FAILED -> new FailedState();
          case DEAD -> new DeadState();
        };

    this.state.takeControl(this, socket);
  }

  public boolean canDownloadPiece(int indexPiece) {
    if (status != Status.UN_CHOKE) {
      throw new RuntimeException("Peer is not in UN_CHOKE state");
    }

    System.out.println(
        STR."can piece #\{
            indexPiece} be downloaded: \{
            bitfield[indexPiece] == -1 ? "yes" : "no"} ");

    //    return bitfield[indexPiece] == -1;
    return true;
  }

  public record Endpoint(String ip, int port) {
    @Override
    public String toString() {
      return STR."\{ip}:\{port}";
    }
  }

  @RequiredArgsConstructor
  public enum Status {
    NEW,
    CONNECTING,
    TALKING,
    CHOKE,

    UN_CHOKE,
    DOWNLOADING,
    FAILED,
    DEAD;
  }

  private record NothingToDoState(String stateName) implements PeerState {
    @Override
    public void takeControl(Peer context, Socket socket) {
      System.out.println(STR."'\{stateName}' is taken the control.");
    }
  }
}
