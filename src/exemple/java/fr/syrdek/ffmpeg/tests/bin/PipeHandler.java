package fr.syrdek.ffmpeg.tests.bin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permet de rediriger le flux sortant d'un process vers un autre flux.
 *
 * @author t0087865
 */
public class PipeHandler implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(PipeHandler.class);
  private final OutputStream os;
  private final InputStream is;

  /**
   * Redirige le flux de is dans os. Les 2 flux sont proprement fermés lorsque plus aucune donnée n'est disponible.
   *
   * @param is
   *          Le flux d'entrée à renvoyer dans os.
   * @param os
   *          Le flux de sortie dans lequel injecter les données reçues depuis is.
   */
  public PipeHandler(final InputStream is, final OutputStream os) {
    this.os = os;
    this.is = is;
  }

  @Override
  public void run() {
    try (final InputStream i = is; final OutputStream o = os) {
      IOUtils.copyLarge(i, o);
    } catch (IOException e) {
      LOG.error("Erreur lors du transfert des données de l'InputStream à l'OutputStream.", e);
    }
  }
}