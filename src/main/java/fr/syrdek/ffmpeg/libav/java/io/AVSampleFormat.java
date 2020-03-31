/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io;

import org.bytedeco.javacpp.avutil;

import fr.syrdek.ffmpeg.libav.java.CEnum;

/**
 * Nombre de bits utilisés pour stocker un sample.
 * 
 * @see https://ffmpeg.org/doxygen/2.4/group__lavu__sampfmts.html
 * @author Syrdek
 *
 */
public enum AVSampleFormat implements CEnum{
  NONE(avutil.AV_SAMPLE_FMT_NONE), //
  U8(avutil.AV_SAMPLE_FMT_U8), //
  S16(avutil.AV_SAMPLE_FMT_S16), //
  S32(avutil.AV_SAMPLE_FMT_S32), //
  FLT(avutil.AV_SAMPLE_FMT_FLT), //
  DBL(avutil.AV_SAMPLE_FMT_DBL), //
  U8P(avutil.AV_SAMPLE_FMT_U8P), //
  S16P(avutil.AV_SAMPLE_FMT_S16P), //
  S32P(avutil.AV_SAMPLE_FMT_S32P), //
  FLTP(avutil.AV_SAMPLE_FMT_FLTP), //
  DBLP(avutil.AV_SAMPLE_FMT_DBLP), //
  NB(avutil.AV_SAMPLE_FMT_NB);

  private final int bitDepth;
  private final int value;

  /**
   * @param value
   *          La valeur de l'enum C correspondante.
   */
  private AVSampleFormat(int value) {
    this.value = value;
    this.bitDepth = avutil.av_get_bytes_per_sample(value) * 8;
  }

  /**
   * @return the value
   */
  public int value() {
    return value;
  }
  
  /**
   * @return Le nombre de bits dans un échantillon.
   */
  public int getBitDepth() {
    return bitDepth;
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
  
  /**
   * @return La représentation textuelle de l'enum.
   */
  public String toString() {
    return new StringBuilder(name()).append("(n°").append(value).append(", ").append(bitDepth).append("bits)").toString();
  }
}
