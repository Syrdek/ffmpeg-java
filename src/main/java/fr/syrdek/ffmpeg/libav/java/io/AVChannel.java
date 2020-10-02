package fr.syrdek.ffmpeg.libav.java.io;

import org.bytedeco.ffmpeg.global.avutil;

import fr.syrdek.ffmpeg.libav.java.CLongFlag;

/**
 * Drapeaux de s'appliquant à un format audio/video.
 *
 * @see https://libav.org/documentation/doxygen/master/channel__layout_8h.html
 *
 * @author Syrdek
 */
public enum AVChannel implements CLongFlag {
  FRONT_LEFT(avutil.AV_CH_FRONT_LEFT), FRONT_RIGHT(avutil.AV_CH_FRONT_RIGHT), FRONT_CENTER(
      avutil.AV_CH_FRONT_CENTER), LOW_FREQUENCY(avutil.AV_CH_LOW_FREQUENCY), BACK_LEFT(
          avutil.AV_CH_BACK_LEFT), BACK_RIGHT(avutil.AV_CH_BACK_RIGHT), FRONT_LEFT_OF_CENTER(
              avutil.AV_CH_FRONT_LEFT_OF_CENTER), FRONT_RIGHT_OF_CENTER(
                  avutil.AV_CH_FRONT_RIGHT_OF_CENTER), BACK_CENTER(avutil.AV_CH_BACK_CENTER), SIDE_LEFT(
                      avutil.AV_CH_SIDE_LEFT), SIDE_RIGHT(avutil.AV_CH_SIDE_RIGHT), TOP_CENTER(
                          avutil.AV_CH_TOP_CENTER), TOP_FRONT_LEFT(avutil.AV_CH_TOP_FRONT_LEFT), TOP_FRONT_CENTER(
                              avutil.AV_CH_TOP_FRONT_CENTER), TOP_FRONT_RIGHT(
                                  avutil.AV_CH_TOP_FRONT_RIGHT), TOP_BACK_LEFT(
                                      avutil.AV_CH_TOP_BACK_LEFT), TOP_BACK_CENTER(
                                          avutil.AV_CH_TOP_BACK_CENTER), TOP_BACK_RIGHT(
                                              avutil.AV_CH_TOP_BACK_RIGHT), STEREO_LEFT(
                                                  avutil.AV_CH_STEREO_LEFT), STEREO_RIGHT(
                                                      avutil.AV_CH_STEREO_RIGHT), WIDE_LEFT(
                                                          avutil.AV_CH_WIDE_LEFT), WIDE_RIGHT(
                                                              avutil.AV_CH_WIDE_RIGHT), SURROUND_DIRECT_LEFT(
                                                                  avutil.AV_CH_SURROUND_DIRECT_LEFT), SURROUND_DIRECT_RIGHT(
                                                                      avutil.AV_CH_SURROUND_DIRECT_RIGHT), LOW_FREQUENCY_2(
                                                                          avutil.AV_CH_LOW_FREQUENCY_2), LAYOUT_NATIVE(
                                                                              avutil.AV_CH_LAYOUT_NATIVE);

  /**
   * Retrouve le {@link AVChannel} ayant la valeur donnée.
   *
   * @param value
   *          La valeur recherchée.
   * @return L'enum correspondant. <code>null</code> si la valeur donnée ne correspond à aucune valeur connue.
   */
  public static AVChannel get(long value) {
    for (AVChannel v : values()) {
      if (v.value == value) {
        return v;
      }
    }
    return null;
  }

  private final long value;

  private AVChannel(long value) {
    this.value = value;
  }

  @Override
  public long value() {
    return value;
  }

  /**
   * @return La représentation textuelle de l'enum.
   */
  @Override
  public String toString() {
    return new StringBuilder(name()).append("(").append(value).append(")").toString();
  }
}