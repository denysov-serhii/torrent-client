package org.desktop2.torrent.client.peer;

import java.net.Socket;

public class FailedState implements PeerState {
  @Override
  public void takeControl(Peer context, Socket socket) {
    //    System.out.println("Peer is failed.");
    context.moveStatusTo(Peer.Status.DEAD);
  }
}
