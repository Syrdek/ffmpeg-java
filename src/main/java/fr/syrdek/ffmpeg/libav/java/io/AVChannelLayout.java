package fr.syrdek.ffmpeg.libav.java.io;

import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.bytedeco.ffmpeg.global.avutil;

import fr.syrdek.ffmpeg.libav.java.CLongFlag;

/**
 * Drapeaux de s'appliquant à un format audio/video.
 *
 * Généré via :<br>
 * wget --quiet -O - https://github.com/FFmpeg/FFmpeg/blob/master/libavutil/channel_layout.h | sed -e 's/>/>\n/g' -e
 * 's/</\n</g' | sed -nr 's#^(AV_CH_LAYOUT_(\S+))$#LAYOUT_\2(avutil.\1), // \2#p'
 *
 * @see https://libav.org/documentation/doxygen/master/channel__layout_8h.html
 *
 * @author Syrdek
 */
public enum AVChannelLayout implements CLongFlag {
  // On préfixe par LAYOUT_ chaque valeur pour éviter d'avoir des valeurs commençant par un nombre.
  LAYOUT_NATIVE(avutil.AV_CH_LAYOUT_NATIVE), // NATIVE
  LAYOUT_MONO(avutil.AV_CH_LAYOUT_MONO), // MONO
  LAYOUT_STEREO(avutil.AV_CH_LAYOUT_STEREO), // STEREO
  LAYOUT_2POINT1(avutil.AV_CH_LAYOUT_2POINT1), // 2POINT1
  LAYOUT_2_1(avutil.AV_CH_LAYOUT_2_1), // 2_1
  LAYOUT_SURROUND(avutil.AV_CH_LAYOUT_SURROUND), // SURROUND
  LAYOUT_3POINT1(avutil.AV_CH_LAYOUT_3POINT1), // 3POINT1
  LAYOUT_4POINT0(avutil.AV_CH_LAYOUT_4POINT0), // 4POINT0
  LAYOUT_4POINT1(avutil.AV_CH_LAYOUT_4POINT1), // 4POINT1
  LAYOUT_2_2(avutil.AV_CH_LAYOUT_2_2), // 2_2
  LAYOUT_QUAD(avutil.AV_CH_LAYOUT_QUAD), // QUAD
  LAYOUT_5POINT0(avutil.AV_CH_LAYOUT_5POINT0), // 5POINT0
  LAYOUT_5POINT1(avutil.AV_CH_LAYOUT_5POINT1), // 5POINT1
  LAYOUT_5POINT0_BACK(avutil.AV_CH_LAYOUT_5POINT0_BACK), // 5POINT0_BACK
  LAYOUT_5POINT1_BACK(avutil.AV_CH_LAYOUT_5POINT1_BACK), // 5POINT1_BACK
  LAYOUT_6POINT0(avutil.AV_CH_LAYOUT_6POINT0), // 6POINT0
  LAYOUT_6POINT0_FRONT(avutil.AV_CH_LAYOUT_6POINT0_FRONT), // 6POINT0_FRONT
  LAYOUT_HEXAGONAL(avutil.AV_CH_LAYOUT_HEXAGONAL), // HEXAGONAL
  LAYOUT_6POINT1(avutil.AV_CH_LAYOUT_6POINT1), // 6POINT1
  LAYOUT_6POINT1_BACK(avutil.AV_CH_LAYOUT_6POINT1_BACK), // 6POINT1_BACK
  LAYOUT_6POINT1_FRONT(avutil.AV_CH_LAYOUT_6POINT1_FRONT), // 6POINT1_FRONT
  LAYOUT_7POINT0(avutil.AV_CH_LAYOUT_7POINT0), // 7POINT0
  LAYOUT_7POINT0_FRONT(avutil.AV_CH_LAYOUT_7POINT0_FRONT), // 7POINT0_FRONT
  LAYOUT_7POINT1(avutil.AV_CH_LAYOUT_7POINT1), // 7POINT1
  LAYOUT_7POINT1_WIDE(avutil.AV_CH_LAYOUT_7POINT1_WIDE), // 7POINT1_WIDE
  LAYOUT_7POINT1_WIDE_BACK(avutil.AV_CH_LAYOUT_7POINT1_WIDE_BACK), // 7POINT1_WIDE_BACK
  LAYOUT_OCTAGONAL(avutil.AV_CH_LAYOUT_OCTAGONAL), // OCTAGONAL
  LAYOUT_HEXADECAGONAL(avutil.AV_CH_LAYOUT_HEXADECAGONAL), // HEXADECAGONAL
  LAYOUT_STEREO_DOWNMIX(avutil.AV_CH_LAYOUT_STEREO_DOWNMIX); // STEREO_DOWNMIX

  /**
   * Retrouve les {@link AVChannel} faisant partie du layout donné. <br>
   * La valeur n'a pas besoin d'appartenir à un layout connu pour être décomposée.
   *
   * @param value
   *          La valeur du layout.
   * @return Les channels du layout.
   */
  public static Set<AVChannel> decompose(long value) {
    final Set<AVChannel> channels = new HashSet<>();
    for (final AVChannel v : AVChannel.values()) {
      if (CLongFlag.isIn(value, v)) {
        channels.add(v);
      }
    }
    return channels;
  }

  /**
   * Retrouve le {@link AVChannelLayout} ayant la valeur donnée.
   *
   * @param value
   *          La valeur recherchée.
   * @return L'enum correspondant. <code>null</code> si la valeur donnée ne correspond à aucune valeur connue.
   */
  public static AVChannelLayout get(long value) {
    for (AVChannelLayout v : values()) {
      if (v.value == value) {
        return v;
      }
    }
    return null;
  }

  /**
   * @param La
   *          valeur d'un layout.
   * @return La représentation textuelle du layout correspondant.
   */
  public static String toString(long value) {
    final AVChannelLayout layout = get(value);
    if (layout == null) {
      final Set<AVChannel> channels = decompose(value);
      if (channels.isEmpty()) {
        return new StringBuilder("LAYOUT_INCONNU(").append(value).append(")").toString();
      }
      return toString("LAYOUT_INCONNU", channels);
    }
    return layout.toString();
  }

  /**
   * @param name
   *          Le nom du layout.
   * @param channels
   *          Les canaux de layout.
   * @return La représentation textuelle du layout.
   */
  private static String toString(final String name, final Set<AVChannel> channels) {
    final StringBuilder b = new StringBuilder(name);
    b.append(channels.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]")));
    return b.toString();
  }

  private final Set<AVChannel> channels;
  private final long value;

  private AVChannelLayout(long value) {
    this.value = value;
    channels = unmodifiableSet(decompose(value));
  }

  @Override
  public long value() {
    return value;
  }

  /**
   * @return Les channels composant ce layout.
   */
  public Set<AVChannel> getChannels() {
    return channels;
  }

  /**
   * @return La représentation textuelle du layout.
   */
  @Override
  public String toString() {
    return toString(name(), channels);
  }
}