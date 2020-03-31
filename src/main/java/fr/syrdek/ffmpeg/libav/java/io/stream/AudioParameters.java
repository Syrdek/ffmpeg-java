/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io.stream;

import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;

import fr.syrdek.ffmpeg.libav.java.io.AVFormatFlag;
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
  private final Integer bitRate;
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
    if (sampleFormat != null) {
      this.bitRate = avutil.av_get_bytes_per_sample(sampleFormat.value()) * sampleRate;
    } else {
      this.bitRate = null;
    }
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
      final Integer bitRate) {
    this.codec = codec;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.bitRate = bitRate;
    this.sampleFormat = null;
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
   * @return the bitRate
   */
  public Integer getBitRate() {
    return bitRate;
  }
}
