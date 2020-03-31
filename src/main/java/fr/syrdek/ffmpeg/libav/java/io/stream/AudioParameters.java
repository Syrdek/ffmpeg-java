/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io.stream;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVRational;

import fr.syrdek.ffmpeg.libav.java.io.AVSampleFormat;

/**
 * Classe immuable décrivant les paramètres d'un flux audio. Doit être construit en utilisant un {@link AudioParameters.Builder}.
 * 
 * @author Syrdek
 */
public class AudioParameters {
  private final String codec;
  private final Integer channels;
  private final Long channelsLayout;
  private final Integer sampleRate;
  private final AVSampleFormat sampleFormat;

  /**
   * Construit des paramètres de flux audio. Tout ou partie des paramètres peuvent être nuls.<br>
   * Dans
   * 
   * @param codec
   *          Le nom du codec a utiliser.
   */
  public AudioParameters(final String codec) {
    this(codec, null, null, null, (AVSampleFormat) null);
  }

  /**
   * Construit des paramètres de flux audio. Tout ou partie des paramètres peuvent être nuls.
   * 
   * @param codec
   *          Le nom du codec a utiliser.
   * @param channels
   *          Le nombre de canaux.
   * @param channelsLayout
   *          La disposition des canaux.
   * @param sampleRate
   *          Le nombre de samples par seconde.
   * @param sampleFormat
   *          Le format des samples.
   */
  public AudioParameters(
      final String codec,
      final Integer channels,
      final Long channelsLayout,
      final Integer sampleRate,
      final AVSampleFormat sampleFormat) {
    this.codec = codec;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.sampleFormat = sampleFormat;
  }

  /**
   * Construit des paramètres de flux audio. Tout ou partie des paramètres peuvent être nuls.
   * 
   * @param codec
   *          Le nom du codec a utiliser.
   * @param channels
   *          Le nombre de canaux.
   * @param channelsLayout
   *          La disposition des canaux.
   * @param sampleRate
   *          Le nombre de samples par seconde.
   * @param bitRate
   *          Le nombre de bits par seconde.
   */
  public AudioParameters(
      final String codec,
      final Integer channels,
      final Long channelsLayout,
      final Integer sampleRate,
      final Long bitRate) {
    this.codec = codec;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.sampleFormat = null;
  }

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
   * @return the codec
   */
  public String getCodec() {
    return codec;
  }

  /**
   * @return the channels
   */
  public Integer getChannels() {
    return channels;
  }

  /**
   * @return the channelsLayout
   */
  public Long getChannelsLayout() {
    return channelsLayout;
  }

  /**
   * @return the sampleRate
   */
  public Integer getSampleRate() {
    return sampleRate;
  }

  /**
   * @return the sampleFormat
   */
  public AVSampleFormat getSampleFormat() {
    return sampleFormat;
  }

  /**
   * @return the sampleFormat
   */
  public Integer getSampleFormatValue() {
    if (sampleFormat != null) {
      return sampleFormat.value();
    }
    return null;
  }
}
