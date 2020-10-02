/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.io.stream;

import java.text.MessageFormat;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;

import fr.syrdek.ffmpeg.libav.java.io.AVPixFormat;

/**
 * Classe immuable décrivant les paramètres d'un flux video.<br>
 * Doit être construit en utilisant un {@link VideoParameters.Builder}.
 *
 * @author t0087865
 */
public class VideoParameters implements CodecContextParameters {
  private final int codec;
  private final Long bitRate;
  private final Integer width;
  private final Integer height;
  private final Integer frameRate;
  private final AVPixFormat pixFormat;

  /**
   * Construit un nouveau VideoParameters
   *
   * @param codec
   *          Le codec
   * @param bitRate
   *          Le bitRate
   * @param width
   *          Le width
   * @param height
   *          Le height
   * @param frameRate
   *          Le frameRate
   * @param pixFormat
   *          Le pixFormat
   */
  private VideoParameters(final int codecId, final Long bitRate, final Integer width, final Integer height,
      final Integer frameRate, final AVPixFormat pixFormat) {
    codec = codecId;
    this.bitRate = bitRate;
    this.width = width;
    this.height = height;
    this.frameRate = frameRate;
    this.pixFormat = pixFormat;
  }

  /**
   *
   *
   * @return codec Le codec
   */
  @Override
  public int getCodec() {
    return codec;
  }

  /**
   *
   *
   * @return bitRate Le bitRate
   */
  @Override
  public Long getBitRate() {
    return bitRate;
  }

  /**
   *
   *
   * @return width Le width
   */
  public Integer getWidth() {
    return width;
  }

  /**
   *
   *
   * @return height Le height
   */
  public Integer getHeight() {
    return height;
  }

  /**
   *
   *
   * @return frameRate Le frameRate
   */
  public Integer getFrameRate() {
    return frameRate;
  }

  /**
   *
   *
   * @return pixFormat Le pixFormat
   */
  public AVPixFormat getPixFormat() {
    return pixFormat;
  }

  /**
   * Pousse les paramètres vidéo dans un contexte d'encodage.
   *
   * @param codecCtx
   *          Le contexte dans lequel copier les paramètres vidéo.
   */
  @Override
  public void pushTo(final AVCodecContext context) {
    context.codec_id(codec);
    context.bit_rate(bitRate);
    context.height(height);
    context.width(width);
    CodecContextParameters.computeTimeBase(frameRate, context.time_base());
    context.pix_fmt(pixFormat.value());
  }

  /**
   * Permet la construction de VideoParameters.
   */
  public static final class Builder {
    private int codec;
    private Long bitRate;
    private Integer width;
    private Integer height;
    private Integer frameRate;
    private AVPixFormat pixFormat;

    /**
     *
     *
     * @param codec
     *          Le codec
     */
    public final Builder withCodec(final int codecId) {
      codec = codecId;
      return this;
    }

    /**
     *
     *
     * @param codec
     *          Le codec
     */
    public final Builder withCodec(final String codec) {
      this.codec = CodecContextParameters.codecIdFromName(codec);
      return this;
    }

    /**
     *
     *
     * @param bitRate
     *          Le bitRate
     */
    public final Builder withBitRate(final Long bitRate) {
      this.bitRate = bitRate;
      return this;
    }

    /**
     *
     *
     * @param width
     *          Le width
     */
    public final Builder withWidth(final Integer width) {
      this.width = width;
      return this;
    }

    /**
     *
     *
     * @param height
     *          Le height
     */
    public final Builder withHeight(final Integer height) {
      this.height = height;
      return this;
    }

    /**
     *
     *
     * @param frameRate
     *          Le frameRate
     */
    public final Builder withFrameRate(final Integer frameRate) {
      this.frameRate = frameRate;
      return this;
    }

    /**
     *
     *
     * @param pixFormat
     *          Le pixFormat
     */
    public final Builder withPixFormat(final AVPixFormat pixFormat) {
      this.pixFormat = pixFormat;
      return this;
    }

    /**
     * Récupère les paramètres depuis le contexte vidéo donné.
     *
     * @param context
     *          Le contexte duquel extraire les paramètres vidéo.
     * @return Cette instance.
     */
    public Builder pullFrom(AVCodecContext context) {
      codec = context.codec_id();
      bitRate = context.bit_rate();
      height = context.height();
      width = context.width();
      frameRate = context.framerate().den();
      pixFormat = AVPixFormat.get(context.pix_fmt());
      return this;
    }

    /**
     * @return Le VideoParameters construit a partir des paramètres donnés.
     */
    public final VideoParameters build() {
      validate();
      return new VideoParameters(codec, bitRate, width, height, frameRate, pixFormat);
    }

    /**
     * Vérifie que les paramètres sont complets.
     */
    public void validate() {
      if (codec == 0) {
        throw new IllegalArgumentException("Le codec doit être précisé dans les paramètres video.");
      }
      if (frameRate == null) {
        throw new IllegalArgumentException("Le framerate doit être précisé dans les paramètres video.");
      }
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
        "VideoParameters [codec={0}({1}), bitRate={2}, width={3}, height={4}, frameRate={5}, pixFormat={6}]",
        getCodecName(), codec, bitRate, width, height, frameRate, pixFormat);
  }
}