package fr.syrdek.ffmpeg.tests;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Syrdek
 *
 */
public class Utils {
  private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

  public static String argOrDefault(final String[] args, final int argnum, final String defaultval) {
    if (args.length > argnum) {
      return args[argnum];
    }
    return defaultval;
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
}
