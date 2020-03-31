/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io;

import org.bytedeco.javacpp.avutil;

/**
 * Nombre de bits utilisés pour stocker un sample.
 * 
 * @see https://ffmpeg.org/doxygen/2.4/group__lavu__sampfmts.html
 * @author Syrdek
 *
 */
public enum AVSampleFormat {
  AV_SAMPLE_FMT_NONE(avutil.AV_SAMPLE_FMT_NONE), //
  AV_SAMPLE_FMT_U8(avutil.AV_SAMPLE_FMT_U8), //
  AV_SAMPLE_FMT_S16(avutil.AV_SAMPLE_FMT_S16), //
  AV_SAMPLE_FMT_S32(avutil.AV_SAMPLE_FMT_S32), //
  AV_SAMPLE_FMT_FLT(avutil.AV_SAMPLE_FMT_FLT), //
  AV_SAMPLE_FMT_DBL(avutil.AV_SAMPLE_FMT_DBL), //
  AV_SAMPLE_FMT_U8P(avutil.AV_SAMPLE_FMT_U8P), //
  AV_SAMPLE_FMT_S16P(avutil.AV_SAMPLE_FMT_S16P), //
  AV_SAMPLE_FMT_S32P(avutil.AV_SAMPLE_FMT_S32P), //
  AV_SAMPLE_FMT_FLTP(avutil.AV_SAMPLE_FMT_FLTP), //
  AV_SAMPLE_FMT_DBLP(avutil.AV_SAMPLE_FMT_DBLP), //
  AV_SAMPLE_FMT_NB(avutil.AV_SAMPLE_FMT_NB);

  private final int value;

  /**
   * @param value
   *          La valeur de l'enum C correspondante.
   */
  private AVSampleFormat(int value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public int value() {
    return value;
  }

  /**
   * Retrouve le {@link AVSampleFormat} ayant la valeur donnée.
   * 
   * @param value
   *          La valeur recherchée.
   * @return L'enum correspondant. <code>null</code> si la valeur donnée ne correspond à aucun format connu.
   */
  public static AVSampleFormat get(int value) {
    for (AVSampleFormat v : values()) {
      if (v.value == value) {
        return v;
      }
    }
    return null;
  }
}
