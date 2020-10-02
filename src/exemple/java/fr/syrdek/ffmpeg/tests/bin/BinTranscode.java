/**
 *
 */
package fr.syrdek.ffmpeg.tests.bin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author t0087865
 *
 */
public class BinTranscode implements Callable<Void> {
  private static final Logger LOG = LoggerFactory.getLogger(BinTranscode.class);
  private static final String FFMPEG = "src/main/bin/ffmpeg";

  /**
   * @param args
   * @throws InterruptedException
   */
  public static void main(final String[] args) throws InterruptedException {
    if (args.length < 4) {
      LOG.error("Usage: BinTranscode NB_THREADS NB_JOBS OUT_DIR VIDEO_1 [VIDEO_2 [...]]");
      System.exit(1);
    }

    final int nbThreads = Integer.parseInt(args[0]);
    final int nbConversions = Integer.parseInt(args[1]);
    final String outputDirectory = args[2];
    final List<String> pathes = Arrays.stream(args).skip(3).collect(Collectors.toList());

    final ExecutorService conversionPool = Executors.newFixedThreadPool(nbThreads);

    long start = System.currentTimeMillis();
    LOG.info("Conversion de {} fichiers sur {} threads...", nbConversions, nbThreads);
    for (int i = 0; i < nbConversions; i++) {
      conversionPool.submit(new BinTranscode(
          pathes.get(i % pathes.size()),
          outputDirectory));
    }

    conversionPool.shutdown();
    conversionPool.awaitTermination(1l, TimeUnit.DAYS);
    LOG.info("OK, fini en {}ms.", System.currentTimeMillis() - start);
    System.exit(0);
  }

  private final String convertedPath;
  private final String rawPath;

  /**
   * @param rawPath
   *          Le chemin vers la vidéo à convertir.
   * @param convertedPath
   *          Le chemin où écrire la vidéo convertie.
   */
  public BinTranscode(final String rawPath, final String convertedPath) {
    this.convertedPath = convertedPath;
    this.rawPath = rawPath;
  }

  @Override
  public Void call() throws Exception {
    // ./ffmpeg -i this.videoPath -strict -2 -ar 44100 -aq 8 -acodec vorbis -ac 2 -vcodec theora -s 480x320
    final ProcessBuilder builder = new ProcessBuilder(
        FFMPEG,
        // Lit la video depuis stdin.
        "-i", "-",
        // Permet d'encoder en vorbis en activant les fonctionnalités expérimentales.
        "-strict", "-2",
        // Sample rate audio.
        "-ar", "44100",
        // Qualité audio.
        "-aq", "8",
        // Channels audio stéréo.
        "-ac", "2",
        // Codec audio.
        "-acodec", "vorbis",
        // Résolution vidéo.
        "-s", "480x320",
        // Codec vidéo.
        "-vcodec", "mpeg4",
        // Modifie le nombre de FPS.
        "-filter:v", "fps=fps=24",
        // Format conteneur.
        "-f", "matroska",
        // Ecrit sur stdout.
        "-");

    if (LOG.isDebugEnabled()) {
      LOG.debug("Commande ffmpeg : {}", builder.command().stream().collect(Collectors.joining(" ")));
    }

    try (final InputStream input = new FileInputStream(rawPath);
        final OutputStream out = new FileOutputStream(convertedPath)) {

      final Process process = builder.start();
      final String threadName = Thread.currentThread().getName();

      // Traite le flux stdout qui contient les données audio/vidéo converties.
      final PipeHandler stdoutHandler = new PipeHandler(process.getInputStream(), out);
      final Thread stdoutThread = new Thread(stdoutHandler, threadName + "-stdout-handler");
      stdoutThread.start();

      // Traite le flux stderr qui contient les informations annexes sur les données converties.
      final FFMpegInfoHandler stderrHandler = new FFMpegInfoHandler(process.getErrorStream());
      final Thread stderrThread = new Thread(stderrHandler, threadName + "-stderr-handler");
      stderrThread.start();

      final long start = System.currentTimeMillis();

      try (final OutputStream o = process.getOutputStream()) {
        IOUtils.copyLarge(input, process.getOutputStream());
      }

      final int result = process.waitFor();
      stdoutThread.join();
      stderrThread.join();

      final long duration = System.currentTimeMillis() - start;

      if (result != 0) {
        LOG.error(
            "Echec de conversion du flux par FFMpeg : {}\n ## Input info : {}\n ## Output info : {}\n ## Mapping info : {}\n ## Conversion info : {}\n{}\n{}",
            stderrHandler.getVersionInfo(),
            stderrHandler.getInputMediaInfo(),
            stderrHandler.getOutputMediaInfo(),
            stderrHandler.getMappingMediaInfo(),
            stderrHandler.getConversionInfo(),
            stderrHandler.getSummary(),
            stderrHandler.getOtherInfo());
      } else {
        LOG.info("Conversion des réussie\n Audio input:{} Video input:{} En {}ms ({})",
            stderrHandler.getAudioCodec(),
            stderrHandler.getVideoCodec(),
            duration,
            stderrHandler.getSummary());
      }
    }

    return null;
  }
}
