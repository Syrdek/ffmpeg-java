/**
 *
 */
package fr.syrdek.ffmpeg.tests.javacpp.impl.decode;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.stream.AudioParameters;

/**
 * Permet de décoder un flux.
 *
 * @author t0087865
 */
public class AudioDecoder extends Decoder {
  private static final Logger LOG = LoggerFactory.getLogger(AudioDecoder.class);

  /**
   *
   * @param formatCtx
   *          Format duquel est extrait le flux a décoder.
   * @param stream
   *          Le flux à décoder.
   * @param codec
   *          Le codec à utiliser pour décoder le flux.
   * @param codecCtx
   *          Le contexte de décodage du flux.
   */
  public AudioDecoder(final AVFormatContext formatCtx, final AVStream stream, final AVCodec codec,
      final AVCodecContext codecCtx) {
    super(formatCtx, stream, codec, codecCtx);
  }

  @Override
  public Media getMedia() {
    return Media.AUDIO;
  }

  public AudioParameters getParameters() {
    return new AudioParameters.Builder()
        .pullFrom(codecCtx)
        .build();
  }
}
