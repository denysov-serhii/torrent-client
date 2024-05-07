package org.desktop2.torrent.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

class TorrentRunnerTest {
  public static final String FILE_TO_DOWNLOAD = "test-file-to-download-via-torrent";

  @Test
  @SneakyThrows
  void handle() {
    //export PATH=$PATH:/Users/serhiidenisov/Downloads/apache-maven-3.9.6

    val tmpConfigDir = Files.createTempDirectory("torrent-config");
    val tmpDownloadsDir = Files.createTempDirectory("torrent-downloads");

    int torrentPort = 6881;
    GenericContainer qbittorrent =
        (GenericContainer)
            new GenericContainer("lscr.io/linuxserver/qbittorrent:latest")
                .withEnv("PUID", "1000")
                .withEnv("PGID", "1000")
                .withEnv("TZ", "Etc/UTC")
                .withEnv("WEBUI_PORT", "8080")
                .withEnv("TORRENTING_PORT", "6881")
                .withExposedPorts(8080, torrentPort)
                .withFileSystemBind(tmpConfigDir.toString(), "/config")
                .withFileSystemBind(tmpDownloadsDir.toString(), "/downloads")
                .waitingFor(Wait.forListeningPort());

    qbittorrent.start();

    URI uri = new URI(STR."http://\{qbittorrent.getHost()}:\{torrentPort}/announce");

    ClassLoader classLoader = getClass().getClassLoader();
    File fileToDownload = new File(classLoader.getResource(FILE_TO_DOWNLOAD).getFile());

    val torrentFile = generateTorrentFileFor(fileToDownload, uri);

    qbittorrent.withCopyFileToContainer(
        MountableFile.forHostPath(Paths.get(torrentFile)),
        STR."/downloads/\{FILE_TO_DOWNLOAD}.torrent");

    qbittorrent.stop();
  }

  @SneakyThrows
  private String generateTorrentFileFor(File file, URI uri) {
    val torrent = com.turn.ttorrent.common.Torrent.create(file, uri, "MyClient");

    val tmpTorrentFile = Files.createTempFile("torrent-testing", ".torrent");
    FileOutputStream fos = new FileOutputStream(tmpTorrentFile.toFile());
    fos.write(torrent.getEncoded());
    fos.close();

    return tmpTorrentFile.toString();
  }
}
