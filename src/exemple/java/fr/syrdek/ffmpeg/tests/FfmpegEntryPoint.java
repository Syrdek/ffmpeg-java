package fr.syrdek.ffmpeg.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.tests.ffmpeg4j.FFMP4JTranscoder;
import fr.syrdek.ffmpeg.tests.jav.JAVTransmux;

/**
 * 
 * @author Syrdek
 *
 */
public class FfmpegEntryPoint {
  private static final Logger LOG = LoggerFactory.getLogger(FfmpegEntryPoint.class);

  public static void ffmpeg4jTest(InputStream in, OutputStream out) throws Exception {
    LOG.info("Décodage via Ffmpeg4j");
    new FFMP4JTranscoder().transcode(in, out);
  }

  public static void jcppTest(InputStream in, OutputStream out) throws Exception {
    LOG.info("Décodage via JavaCpp");
    // new JAVInfo().printInfos(in);
//    new JCPPTransmux().withFormatName("matroska").transmux(in, out);
    new JAVTransmux().transmux(in, out, "matroska");
  }

  public static void cleanup() {
    final File cwd = new File(System.getProperty("user.dir"));
    // Supprime les fichiers hs_err_pid***.log pour trouver facilement celui a analyser en cas d'erreur.
    // C'est peut-être mauvais signe de devoir en arriver là...
    for (File f : cwd.listFiles(
        f -> f.isFile() &&
        f.getName().startsWith("hs_err_pid"))) {
      LOG.debug("Suppression de {}", f.getPath());
      f.delete();
    }
  }

  public static void main(String[] args) throws Exception {
    cleanup();

    try (final InputStream in = new FileInputStream("samples/video.mp4");
        final OutputStream out = new FileOutputStream("target/result.mkv")) {
      jcppTest(in, out);
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }
}
