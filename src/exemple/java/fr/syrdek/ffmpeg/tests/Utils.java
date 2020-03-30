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
public class Utils {
  private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

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
}