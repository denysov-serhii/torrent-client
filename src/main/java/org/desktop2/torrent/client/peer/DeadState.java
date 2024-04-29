package org.desktop2.torrent.client.peer;

import java.net.Socket;

public class DeadState implements PeerState {
  @Override
  public void takeControl(Peer context, Socket socket) {
    //    System.out.println("Peer is dead and it should be removed.");
  }
}
