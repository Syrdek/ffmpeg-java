package fr.syrdek.ffmpeg.libav.java;

import org.bytedeco.ffmpeg.global.avutil;

/**
 * Types de media gérés par libav.
 *
 * @author Syrdek
 *
 */
public enum Media {
  AUDIO(avutil.AVMEDIA_TYPE_AUDIO), //
  VIDEO(avutil.AVMEDIA_TYPE_VIDEO), //
  SUBTITLE(avutil.AVMEDIA_TYPE_SUBTITLE), //
  UNKNOWN(avutil.AVMEDIA_TYPE_UNKNOWN);

  private final int value;

  Media(int value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public int value() {
    return value;
  }

  /**
   * @param type
   *          Le type de media sous forme d'enum libav.
   * @return Le Media correspondant a cet id. <code>Media.UNKNOWN</code> si aucun media ne correspond.
   */
  public static Media of(int type) {
    for (final Media m : Media.values()) {
      if (m.value == type) {
        return m;
      }
    }
    return UNKNOWN;
  }
}
