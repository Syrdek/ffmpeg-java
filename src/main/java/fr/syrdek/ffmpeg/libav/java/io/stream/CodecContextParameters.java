package fr.syrdek.ffmpeg.libav.java.io.stream;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;

/**
 * Paramètres d'un contexte d'encodage / décodage.
 *
 * @author t0087865
 */
public interface CodecContextParameters {

  /**
   * Calcule le temps entre 2 samples.<br>
   *
   * @param sampleRate
   *          Le nombre de samples par seconde.
   * @param result
   *          Le temps entre 2 samples, a remplir.
   * @return Le paramètre result.
   */
  public static AVRational computeTimeBase(int sampleRate, AVRational result) {
    return result.num(1).den(sampleRate);
  }

  /**
   * Trouve l'id d'un codec à partir de son nom.
   *
   * @param codec
   *          L'id du codec.
   * @return L'id du codec.
   */
  public static int codecIdFromName(final String codec) {
    try (final AVCodec c = checkAllocation(avcodec.avcodec_find_encoder_by_name(codec),
        "Impossible de trouver le codec {0}.", codec)) {
      return c.id();
    }
  }

  /**
   * @param id
   *          L'id du codec pour lequel retrouver le nom.
   * @return Le nom du codec. <code>null</code> si le codec est introuvable.
   */
  public static String codecNameFromId(final int id) {
    try (final AVCodec c = avcodec.avcodec_find_encoder(id)) {
      if (c != null) {
        return c.name().getString();
      }
      return null;
    }
  }

  /**
   * @return codec Le codec
   */
  int getCodec();

  /**
   * @return bitRate Le bitRate
   */
  Long getBitRate();

  /**
   * Pousse les paramètres vidéo dans un contexte d'encodage.
   *
   * @param codecCtx
   *          Le contexte dans lequel copier les paramètres vidéo.
   */
  void pushTo(final AVCodecContext context);

  /**
   * @return Le nom du codec. <code>null</code> si le codec est introuvable.
   */
  public default String getCodecName() {
    return codecNameFromId(getCodec());
  }

  /**
   * @return Le codec natif. <b>Attention</b>, ce codec doit être fermé en appelant {@link AVCodec#close()} après
   *         utilisation pour libérer la mémoire.
   */
  public default AVCodec getNativeCodec() {
    final int codecId = getCodec();
    return checkAllocation(avcodec.avcodec_find_encoder(codecId),
        "Impossible de trouver le codec {0}.", codecId);
  }
}
