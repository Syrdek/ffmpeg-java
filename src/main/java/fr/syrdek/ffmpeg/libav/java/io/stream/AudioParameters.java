/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io.stream;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil.AVRational;

import fr.syrdek.ffmpeg.libav.java.io.AVSampleFormat;

/**
 * Classe immuable décrivant les paramètres d'un flux audio. Doit être construit
 * en utilisant un {@link AudioParameters.Builder}.
 * 
 * @author Syrdek
 */
public class AudioParameters {
  private final int codecId;
  private final String codec;
  private final Integer channels;
  private final Long channelsLayout;
  private final Integer sampleRate;
  private final AVSampleFormat sampleFormat;

  /**
   * Trouve le nom d'un codec à partir de son id.
   * 
   * @param codecId L'id du codec.
   * @return Le nom du codec.
   */
  private static final String codecNameFromId(final int codecId) {
    return avcodec.avcodec_find_encoder(codecId).name().getString();
  }

  /**
   * Trouve l'id d'un codec à partir de son nom.
   * 
   * @param codec L'id du codec.
   * @return L'id du codec.
   */
  private static final int codecIdFromName(final String codec) {
    return avcodec.avcodec_find_encoder_by_name(codec).id();
  }

  /**
   * Construit des paramètres de flux audio, uniquement en utilisant le nom du
   * codec.
   * 
   * @param codec Le nom du codec a utiliser.
   */
  public AudioParameters(final String codec) {
    this(codec, null, null, null, (AVSampleFormat) null);
  }

  /**
   * Construit des paramètres de flux audio, uniquement en utilisant l'id du
   * codec.
   * 
   * @param codecId L'id du codec a utiliser.
   */
  public AudioParameters(final int codecId) {
    this(codecId, null, null, null, (AVSampleFormat) null);
  }

  /**
   * Construit des paramètres de flux audio. Tout ou partie des paramètres peuvent
   * être nuls.
   * 
   * @param codec          Le nom du codec a utiliser.
   * @param channels       Le nombre de canaux.
   * @param channelsLayout La disposition des canaux.
   * @param sampleRate     Le nombre de samples par seconde.
   * @param sampleFormat   Le format des samples.
   */
  public AudioParameters(final String codec, final Integer channels, final Long channelsLayout,
      final Integer sampleRate, final AVSampleFormat sampleFormat) {
    this.codec = codec;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.sampleFormat = sampleFormat;
    this.codecId = codecIdFromName(codec);
  }

  /**
   * Construit des paramètres de flux audio. Tout ou partie des paramètres peuvent
   * être nuls.
   * 
   * @param codecId        L'id du codec a utiliser.
   * @param channels       Le nombre de canaux.
   * @param channelsLayout La disposition des canaux.
   * @param sampleRate     Le nombre de samples par seconde.
   * @param sampleFormat   Le format des samples.
   */
  public AudioParameters(final int codecId, final Integer channels, final Long channelsLayout, final Integer sampleRate,
      final AVSampleFormat sampleFormat) {
    this.codecId = codecId;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.sampleFormat = sampleFormat;
    this.codec = codecNameFromId(codecId);
  }

  /**
   * Construit des paramètres de flux audio. Tout ou partie des paramètres peuvent
   * être nuls.
   * 
   * @param codec          Le nom du codec a utiliser.
   * @param channels       Le nombre de canaux.
   * @param channelsLayout La disposition des canaux.
   * @param sampleRate     Le nombre de samples par seconde.
   * @param bitRate        Le nombre de bits par seconde.
   */
  public AudioParameters(final String codec, final Integer channels, final Long channelsLayout,
      final Integer sampleRate, final Long bitRate) {
    this.codec = codec;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.sampleFormat = null;
    this.codecId = codecIdFromName(codec);
  }

  /**
   * Construit des paramètres de flux audio. Tout ou partie des paramètres peuvent
   * être nuls.
   * 
   * @param codecId        L'id du codec a utiliser.
   * @param channels       Le nombre de canaux.
   * @param channelsLayout La disposition des canaux.
   * @param sampleRate     Le nombre de samples par seconde.
   * @param bitRate        Le nombre de bits par seconde.
   */
  public AudioParameters(final int codecId, final Integer channels, final Long channelsLayout,
      final Integer sampleRate, final Long bitRate) {
    this.codecId = codecId;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.sampleFormat = null;
    this.codec = codecNameFromId(codecId);
  }

  /**
   * Calcule le temps entre 2 samples.<br>
   * 
   * @param sampleRate Le nombre de samples par seconde.
   * @param result     Le temps entre 2 samples, a remplir.
   * @return Le paramètre result.
   */
  public static AVRational computeTimeBase(int sampleRate, AVRational result) {
    return result.num(1).den(sampleRate);
  }

  /**
   * @return the codecId
   */
  public int getCodecId() {
    return codecId;
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
