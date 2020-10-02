/**
 *
 */
package fr.syrdek.ffmpeg.tests.javacpp.impl.process;

import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.tests.javacpp.impl.BaseProducer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.Producer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils;

/**
 * @author t0087865
 */
public class VideoLimiter extends BaseProducer<AVFrame> implements Consumer<AVFrame> {
  private static final Logger LOG = LoggerFactory.getLogger(VideoLimiter.class);

  private final AVRational sourceFps;
  private final AVRational timebase;
  private final int expectedFps;
  private final int ratioNum;
  private final int ratioDen;

  /**
   * Détermine s'il est nécessaire de limiter les FPS.
   *
   * @param expectedFps
   *          Le nombre de FPS attendu en sortie.
   * @param sourceFps
   *          Le nombre de FPS en entrée.
   * @return <code>true</code> si un {@link VideoLimiter} est nécessaire. <code>false</code> sinon.
   */
  public static final boolean isLimiterNeeded(int expectedFps, AVRational sourceFps) {
    return expectedFps < sourceFps.num() / sourceFps.den();
  }

  /**
   * Installe un limiteur de FPS sur la source de données. Si le nombre de FPS désiré en sortie est supérieur au nombre
   * de FPS en entrée, n'installe rien.
   *
   * @param source
   *          La source de données sur laquelle poser un limiteur de FPS.
   * @param expectedFps
   *          Le nombre de FPS attendu en sortie.
   * @param timebase
   *          L'unité de temps utilisée dans le flux en entrée.
   * @param sourceFps
   *          Le nombre de FPS du flux en entrée.
   * @return Le limiteur de FPS si il est nécessaire de limiter les FPS de la source. Sinon, retourne directement la
   *         source.
   */
  public static final Producer<AVFrame> limitFps(final Producer<AVFrame> source, final int expectedFps,
      final AVRational timebase, final AVRational sourceFps) {
    if (isLimiterNeeded(expectedFps, sourceFps)) {
      LOG.debug("Ajout d'un FPS limiter ({}/{} -> {}/1) sur le flux vidéo.", sourceFps.num(), sourceFps.den(),
          expectedFps);
      // Installe un limiteur et le retourne.
      final VideoLimiter limiter = new VideoLimiter(expectedFps, timebase, sourceFps);
      source.sendTo(limiter);
      return limiter;
    }
    // Sinon, retourne directement la source. Pas besoin d'y ajouter un limiteur.
    return source;
  }

  /**
   * Construit un limiteur de FPS.
   *
   * @param expectedFps
   *          Le nombre de FPS attendu en sortie.
   * @param timebase
   *          L'unité de temps utilisée dans le flux en entrée.
   * @param sourceFps
   *          Le nombre de FPS du flux en entrée.
   */
  public VideoLimiter(final int expectedFps, final AVRational timebase, final AVRational sourceFps) {
    super();
    this.expectedFps = expectedFps;
    this.sourceFps = sourceFps;
    this.timebase = timebase;

    // Simplifie la fraction afin d'obtenir le ratio de nombre de frames a conserver.
    // La simplification est nécessaire car si l'on souhaite passer de 24FPS à 12FPS :
    // - Avec un ratio 12/24, l'algorithme garde 12 images, puis en supprime 12, puis en garde 12, ... Ce qui fait une
    // vidéo non fluide.
    // - Avec un ration 1/2, l'algorithme garde une image, en supprime une, en garde une, ... Ce qui rend la
    // transformation fluide.
    final int pgcd = pgcd(sourceFps.num(), expectedFps);
    // Numérateur du ratio.
    ratioNum = sourceFps.num() / pgcd;
    // Dénominateur du ratio.
    ratioDen = expectedFps / pgcd;
  }

  /**
   * @param a
   *          Un nombre.
   * @param b
   *          Un nombre.
   * @return Le PGCD des 2 nombres.
   */
  public static int pgcd(int a, int b) {
    return b == 0 ? a : pgcd(b, a % b);
  }

  @Override
  public void accept(final AVFrame frame) {
    if (!shouldSkip(frame)) {
      publish(frame);
    }
  }

  /**
   *
   * @param frame
   * @return
   */
  public boolean shouldSkip(final AVFrame frame) {
    // Récupère le timestamp de la frame, c'est-à-dire au bout de combien de temps doit s'afficher la frame dans la
    // vidéo. L'unité est de 1 seconde / timebase.
    final double bets = frame.best_effort_timestamp();
    // Multiplie le timestamp par le timebase pour avoir le timestamp en secondes.
    final double ts = bets * timebase.num() / timebase.den();
    // Trouve le numéro de frame en multipliant le timestamp par le nombre de FPS.
    long frameNum = Math.round(ts * sourceFps.num() / sourceFps.den());

    final double n = frameNum % ratioNum;

    final boolean skip = n >= ratioDen;
    if (LOG.isDebugEnabled()) {
      LOG.debug("Frame timestamp : {} ({}). Numéro de frame {} -> {}. {}.",
          String.format("%.3f", ts), TimeUtils.toHms(ts),
          String.format("%02d", frameNum),
          String.format("%.3f", n),
          skip ? "Ignorée" : "Gardée");
    }

    return skip;
  }
}
