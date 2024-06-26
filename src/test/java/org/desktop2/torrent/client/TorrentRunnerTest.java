package org.desktop2.torrent.client;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.Wait;

class TorrentRunnerTest {
  public static final String FILE_TO_DOWNLOAD = "test-file-to-download-via-torrent";

  @Test
  @SneakyThrows
  void handle() {

    val commonTmpFolder = Files.createTempDirectory("torrent-client-tests");

    val downloadsDir = Paths.get(STR."\{commonTmpFolder}\{File.separator}downloads");
    val torrentFilesDir = Paths.get(STR."\{commonTmpFolder}\{File.separator}torrent-files");

    Files.createDirectory(downloadsDir);
    Files.createDirectory(torrentFilesDir);

    System.out.println(STR."Common folder: \{commonTmpFolder}");
    System.out.println(STR."Downloads files folder: \{downloadsDir}");
    System.out.println(STR."Torrent files folder: \{torrentFilesDir}");

    int torrentPort = 6881;
    TestGenericContainer simpleTrackerContainer =
        new TestGenericContainer("gaydara27/simple-torrent-tracker:1.0.0-SNAPSHOT")
            .withEnv("PORT", torrentPort + "")
            .withFileSystemBind(downloadsDir.toString(), "/downloads", BindMode.READ_WRITE)
            .withFileSystemBind(
                    torrentFilesDir.toString(), "/torrent-files", BindMode.READ_WRITE)
            .withEnv("APP_ARGS", STR."TRACKER /downloads /torrent-files")
            .waitingFor(Wait.forListeningPort());

    simpleTrackerContainer.addFixedExposedPort(torrentPort, torrentPort, InternetProtocol.TCP);
    simpleTrackerContainer.addFixedExposedPort(torrentPort, torrentPort, InternetProtocol.UDP);

    simpleTrackerContainer.start();

    URI uri = new URI(STR."http://\{simpleTrackerContainer.getHost()}:\{torrentPort}/announce");

    ClassLoader classLoader = getClass().getClassLoader();
    File fileToDownload = new File(classLoader.getResource(FILE_TO_DOWNLOAD).getFile());

    System.out.println(STR."Is file\{fileToDownload.toPath()} exist? \{fileToDownload.exists()}");


    Files.copy(fileToDownload.toPath(),concatFolderWithFileName(downloadsDir, fileToDownload), REPLACE_EXISTING);

    val runner = new TorrentRunner();

    try {
      val torrentFile =
              waitForTorrentFile(torrentFilesDir, STR."\{FILE_TO_DOWNLOAD}.torrent", 60).toPath();

      runner.handle(torrentFile.toString(), torrentFile, new TestProgressCounter());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      Files.walk(commonTmpFolder).map(Path::toFile).forEach(File::delete);
    }

    simpleTrackerContainer.stop();
  }

  @SneakyThrows
  private Path generateTorrentFileFor(File file, URI uri) {
    val torrent = com.turn.ttorrent.common.Torrent.create(file, uri, "MyClient");

    val tmpTorrentFile = Files.createTempFile("torrent-testing", ".torrent");
    FileOutputStream fos = new FileOutputStream(tmpTorrentFile.toFile());
    fos.write(torrent.getEncoded());
    fos.close();

    return tmpTorrentFile;
  }

  @SneakyThrows
  private File waitForTorrentFile(Path folder, String targetFile, int timeoutSeconds) {
    WatchService watcher = FileSystems.getDefault().newWatchService();
    folder.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

    System.out.println(STR."Watch Service registered for dir: \{folder}");

    while (true) {
      WatchKey key;
      key = watcher.poll(timeoutSeconds, TimeUnit.SECONDS);

      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        @SuppressWarnings("unchecked")
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path fileName = ev.context();

        System.out.println(STR."\{kind.name()}: \{fileName}");

        if (kind == StandardWatchEventKinds.ENTRY_CREATE
            && fileName.toString().equals(targetFile)) {
          System.out.println(STR."File \{fileName} has been created!");
          return new File(folder + File.separator + fileName);
        }
      }

      boolean valid = key.reset();
      if (!valid) {
        break;
      }
    }

    throw new RuntimeException("Unable to wait for the file");
  }

  private static class TestProgressCounter implements ProgressCounter {

    private static final DecimalFormat df = new DecimalFormat("0.000");
    private int numberOfPieces;
    private AtomicInteger downloadedPieces = new AtomicInteger(0);
    private AtomicInteger failedPieces = new AtomicInteger(0);
    private final Runnable progressPrinter =
        new Runnable() {
          public void run() {
            if (downloadedPieces.get() == 0) {
              return;
            }

            double rate = ((double) numberOfPieces / downloadedPieces.get()) * 100;

            System.out.print(
                STR."Progress: \{
                    df.format(rate)}, All:  Download:\{
                    numberOfPieces} \{
                    downloadedPieces} Failed: \{
                    failedPieces} \r");
          }
        };

    public TestProgressCounter() {
      ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
      executor.scheduleAtFixedRate(progressPrinter, 0, 3, TimeUnit.SECONDS);
    }

    @Override
    public void numberPieces(int numberPieces) {
      numberOfPieces = numberPieces;
      System.out.println("Number of pieces: " + numberPieces);
    }

    @Override
    public void downloadPiece(int numberOfPiece) {
      downloadedPieces.incrementAndGet();
      System.out.println(STR."Piece #\{numberOfPiece} is downloaded");
    }

    @Override
    public void failedToDownloadPiece(int numberOfPiece) {
      failedPieces.incrementAndGet();
      System.out.println(STR."Piece #\{numberOfPiece} is downloaded");
    }
  }

  static class TestGenericContainer extends GenericContainer<TestGenericContainer> {

    public TestGenericContainer(@NonNull String dockerImageName) {
      super(dockerImageName);
    }

    @Override
    public void addFixedExposedPort(int hostPort, int containerPort, InternetProtocol protocol) {
      super.addFixedExposedPort(hostPort, containerPort, protocol);
    }
  }

  private static Path concatFolderWithFileName(Path path, File file) {
    return concatFolderWithFileName(path, file.getName());
  }
  private static Path concatFolderWithFileName(Path path, String fileName) {
    return Paths.get(path + File.separator + fileName);
  }
}
