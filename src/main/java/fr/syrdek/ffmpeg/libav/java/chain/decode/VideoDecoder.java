/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.chain.decode;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.stream.VideoParameters;

/**
 * Permet de décoder un flux.
 *
 * @author t0087865
 */
public class VideoDecoder extends Decoder {
  private static final Logger LOG = LoggerFactory.getLogger(VideoDecoder.class);

  private final int bufsize;

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
  public VideoDecoder(final AVFormatContext formatCtx, final AVStream stream, final AVCodec codec,
      final AVCodecContext codecCtx) {
    super(formatCtx, stream, codec, codecCtx);
    // Construit une frame vidéo (= une image).
    frame.format(codecCtx.pix_fmt());
    frame.width(codecCtx.width());
    frame.height(codecCtx.height());

    // Alloue suffisemment d'espace pour contenir les données de l'image.
    bufsize = avutil.av_image_alloc(
        frame.data(),
        frame.linesize(),
        frame.width(),
        frame.height(),
        frame.format(),
        1);
  }

  @Override
  public Media getMedia() {
    return Media.VIDEO;
  }

  /**
   * @return Les paramètres vidéo
   */
  public VideoParameters getParameters() {
    return new VideoParameters.Builder()
        .pullFrom(codecCtx)
        .build();
  }

  /**
   * @return the bufsize
   */
  public int getBufsize() {
    return bufsize;
  }
}
