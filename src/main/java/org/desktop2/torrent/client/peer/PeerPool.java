package org.desktop2.torrent.client.peer;

import java.util.concurrent.*;
import lombok.SneakyThrows;
import org.desktop2.torrent.client.PeerService;
import org.desktop2.torrent.client.Torrent;

public class PeerPool {

  private static final PeerService peerService = new PeerService();

  private final BlockingQueue<Peer> peers;
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  private PeerPool(BlockingQueue<Peer> peers) {
    this.peers = peers;
  }

  public static PeerPool getInstance(Torrent torrent, String peerId) {
    var peers = peerService.getPeers(torrent, peerId).stream().limit(500).toList();
    return new PeerPool(new ArrayBlockingQueue<>(100, false, peers));
  }

  @SneakyThrows
  public Peer take() {
    Exception exception = new RuntimeException("Cannot get Peer.");
    for (int i = 0; i == i; i++) {
      try {
        System.out.println(STR."trying to get a peer. Attempt # \{i + 1}");
        var peer = peers.take();

        if (peer.getStatus() == Peer.Status.UN_CHOKE) {
          return peer;
        }

        if (peer.getStatus() == Peer.Status.DEAD) {
          continue;
        }

        if (peer.getStatus() == Peer.Status.CHOKE) {
          peer.moveStatusTo(Peer.Status.NEW);
          realise(peer);
        }

        if (peer.getStatus() == Peer.Status.NEW) {
          executorService.execute(
              () -> {
                peer.moveStatusTo(Peer.Status.CONNECTING);
                realise(peer);
              });
        } else {
          realise(peer);
        }

        TimeUnit.MILLISECONDS.sleep(100);
      } catch (InterruptedException e) {
        exception = e;
        System.out.println(STR."Something went wrong with attempt #\{i + 1}, try again... ");
      }
    }

    throw exception;
  }

  public void realise(Peer peer) {
    peers.add(peer);
  }
}
