/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.io.stream;

import java.text.MessageFormat;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;

import fr.syrdek.ffmpeg.libav.java.io.AVChannelLayout;
import fr.syrdek.ffmpeg.libav.java.io.AVSampleFormat;

/**
 * Classe immuable décrivant les paramètres d'un flux audio.<br>
 * Doit être construit en utilisant un {@link AudioParameters.Builder}.
 *
 * @author Syrdek
 */
public class AudioParameters implements CodecContextParameters {
  private final int codec;
  private final Integer channels;
  private final Long channelsLayout;
  private final Integer sampleRate;
  private final Long bitRate;
  private final AVSampleFormat sampleFormat;

  /**
   * Construit un nouveau AudioParameters
   *
   * @param codec
   *          Le codec
   * @param channels
   *          Le channels
   * @param channelsLayout
   *          Le channelsLayout
   * @param sampleRate
   *          Le sampleRate
   * @param bitRate
   *          Le bitRate
   * @param sampleFormat
   *          Le sampleFormat
   */
  private AudioParameters(final int codecId, final Integer channels,
      final Long channelsLayout, final Integer sampleRate, final Long bitRate,
      final AVSampleFormat sampleFormat) {
    codec = codecId;
    this.channels = channels;
    this.channelsLayout = channelsLayout;
    this.sampleRate = sampleRate;
    this.bitRate = bitRate;
    this.sampleFormat = sampleFormat;
  }

  /**
   * @return the codec
   */
  @Override
  public int getCodec() {
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

  /**
   * @return the bitRate
   */
  @Override
  public Long getBitRate() {
    return bitRate;
  }

  @Override
  public void pushTo(AVCodecContext context) {
    context.codec_id(codec);
    context.channels(channels);
    context.channel_layout(channelsLayout);
    context.sample_rate(sampleRate);
    context.sample_fmt(getSampleFormatValue());
    CodecContextParameters.computeTimeBase(sampleRate, context.time_base());
  }

  /**
   * Permet la construction de AudioParameters.
   */
  public static final class Builder {
    private int codec;
    private Integer channels;
    private Long channelsLayout;
    private Integer sampleRate;
    private Long bitRate;
    private AVSampleFormat sampleFormat;

    /**
     * @param codec
     *          Le codec
     */
    public final Builder withCodec(final int codecId) {
      codec = codecId;
      return this;
    }

    /**
     * @param codec
     *          Le codec
     */
    public final Builder withCodec(final String codec) {
      this.codec = CodecContextParameters.codecIdFromName(codec);
      return this;
    }

    /**
     * @param channels
     *          Le channels
     */
    public final Builder withChannels(final Integer channels) {
      this.channels = channels;
      return this;
    }

    /**
     * @param channelsLayout
     *          Le channelsLayout
     */
    public final Builder withChannelsLayout(final AVChannelLayout channelsLayout) {
      this.channelsLayout = channelsLayout.value();
      return this;
    }

    /**
     * @param sampleRate
     *          Le sampleRate
     */
    public final Builder withSampleRate(final Integer sampleRate) {
      this.sampleRate = sampleRate;
      return this;
    }

    /**
     * @param bitRate
     *          Le bitRate
     */
    public final Builder withBitRate(final Long bitRate) {
      this.bitRate = bitRate;
      return this;
    }

    /**
     * @param sampleFormat
     *          Le sampleFormat
     */
    public final Builder withSampleFormat(final AVSampleFormat sampleFormat) {
      this.sampleFormat = sampleFormat;
      return this;
    }

    /**
     * @return Le AudioParameters construit a partir des paramètres donnés
     */
    public final AudioParameters build() {
      validate();
      return new AudioParameters(codec, channels, channelsLayout, sampleRate, bitRate, sampleFormat);
    }

    /**
     * Vérifie que les paramètres sont complets.
     */
    public void validate() {
      if (codec == 0) {
        throw new IllegalArgumentException("Le codec doit être précisé dans les paramètres audio.");
      }
    }

    /**
     * Récupère les paramètres depuis le contexte audio donné.
     *
     * @param context
     *          Le contexte duquel extraire les paramètres audio.
     * @return Cette instance.
     */
    public Builder pullFrom(AVCodecContext context) {
      codec = context.codec_id();
      bitRate = context.bit_rate();
      channels = context.channels();
      channelsLayout = context.channel_layout();
      sampleRate = context.sample_rate();
      sampleFormat = AVSampleFormat.get(context.sample_fmt());
      return this;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MessageFormat.format(
        "AudioParameters [codec={0}({1}), channels={2}, channelsLayout={3}, sampleRate={4}, bitRate={5}, sampleFormat={6}]",
        getCodecName(), codec, channels, channelsLayout, sampleRate, bitRate, sampleFormat);
  }
}
