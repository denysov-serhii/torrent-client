package org.desktop2.torrent.client.peer;

import java.net.Socket;

@FunctionalInterface
public interface PeerState {
  void takeControl(Peer context, Socket socket);
}
