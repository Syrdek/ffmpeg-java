/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.chain.process;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.chain.BaseProducer;
import fr.syrdek.ffmpeg.libav.java.chain.Producer;
import fr.syrdek.ffmpeg.libav.java.io.AVPixFormat;
import fr.syrdek.ffmpeg.libav.java.io.SWSInterpolation;
import fr.syrdek.ffmpeg.libav.java.io.stream.VideoParameters;

/**
 * Permet de redimentionner les images d'une vidéo.<br>
 * Les images consommées ne sont pas modifiées, mais une copie est créée et produite en sortie. Les consommateurs
 * inscrits via {@link Producer#sendTo(Consumer)} recevront la copie redimensionnée des frames de la vidéo.
 *
 * @author t0087865
 */
public class VideoRescaler extends BaseProducer<AVFrame> implements Consumer<AVFrame>, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(VideoRescaler.class);

  private final AVPixFormat srcFormat;
  private final int srcHeight;
  private final int srcWidth;

  private final AVPixFormat dstFormat;
  private final int dstHeight;
  private final int dstWidth;

  private final SWSInterpolation scalingMethod;

  private final SwsContext context;
  private final AVFrame dstFrame;

  /**
   * Construit un redimensionneur de vidéo.
   *
   * @param srcFormat
   *          Le format d'image source.
   * @param srcWidth
   *          La largeur d'image source.
   * @param srcHeight
   *          La hauteur de l'image source.
   * @param dstFormat
   *          Le format d'image à construire.
   * @param dstWidth
   *          La largeur d'image à construire.
   * @param dstHeight
   *          La hauteur de l'image à construire.
   * @param scalingMethod
   *          La méthode d'interpolation à utiliser pour redimensionner l'image.
   */
  public VideoRescaler(AVPixFormat srcFormat, int srcWidth, int srcHeight, AVPixFormat dstFormat, int dstWidth,
      int dstHeight, SWSInterpolation scalingMethod) {
    this.srcFormat = srcFormat;
    this.srcHeight = srcHeight;
    this.srcWidth = srcWidth;

    this.dstFormat = dstFormat;
    this.dstHeight = dstHeight;
    this.dstWidth = dstWidth;

    this.scalingMethod = scalingMethod == null ? SWSInterpolation.DEFAULT : scalingMethod;

    if (LOG.isDebugEnabled()) {
      LOG.debug("Construction du contexte de redimensionnement vidéo {}x{}[{}] --{}--> {}x{}[{}].",
          srcWidth, srcHeight, srcFormat,
          this.scalingMethod,
          dstWidth, dstHeight, dstFormat);
    }

    // On construit une frame qui contiendra l'image redimensionnée.
    dstFrame = checkAllocation(avutil.av_frame_alloc(),
        "Impossible d'allouer une nouvelle frame pour le redimensionnement vidéo.");
    dstFrame.format(dstFormat.value());
    dstFrame.height(dstHeight);
    dstFrame.width(dstWidth);

    // Alloue le tableau d'octets de l'image.
    checkAndThrow(
        avutil.av_image_alloc(dstFrame.data(), dstFrame.linesize(), dstFrame.width(), dstFrame.height(),
            dstFrame.format(),
            32),
        "Impossible d'allouer une image de {}x{}[{}].", dstFrame.width(), dstFrame.height(),
        dstFormat);

    // Construit le contexte qui sera utilisé pour procéder au redimensionnement.
    context = checkAllocation(swscale.sws_getContext(
        srcWidth, srcHeight, srcFormat.value(),
        dstWidth, dstHeight, dstFormat.value(),
        this.scalingMethod.value(), null, null, (double[]) null),
        "Impossible de construire le contexte de redimensionnement vidéo {}x{} [{}] --{}--> {}x{} [{}].",
        srcWidth, srcHeight, srcFormat,
        this.scalingMethod,
        dstWidth, dstHeight, dstFormat);

  }

  /**
   * Construit un redimensionneur de vidéo.
   *
   * @param src
   *          Les paramètres des images source.
   * @param destination
   *          Les paramètres d'image recherchés.
   * @param scalingMethod
   *          La méthode d'interpolation à utiliser pour redimensionner l'image.
   */
  public VideoRescaler(final VideoParameters src, final VideoParameters destination,
      final SWSInterpolation scalingMethod) {
    this(
        src.getPixFormat(),
        src.getWidth(),
        src.getHeight(),
        destination.getPixFormat(),
        destination.getWidth(),
        destination.getHeight(),
        scalingMethod);
  }

  @Override
  public void accept(final AVFrame frame) {
    // Construit une version redimensionnée de frame dans dstFrame.
    checkAndThrow(
        swscale.sws_scale(context,
            frame.data(), frame.linesize(), 0, frame.height(),
            dstFrame.data(), dstFrame.linesize()),
        "Impossible de redimensionner l'image source.");

    // Copie les DTS / PTS dans la frame redimensionnée.
    dstFrame.pts(frame.pts());
    dstFrame.pkt_dts(frame.pkt_dts());
    dstFrame.pkt_duration(frame.pkt_duration());
    dstFrame.best_effort_timestamp(frame.best_effort_timestamp());

    publish(dstFrame);
  }

  @Override
  public void close() {
    avutil.av_frame_free(dstFrame);
    swscale.sws_freeContext(context);
  }

  /**
   * @return the srcFormat
   */
  public AVPixFormat getSrcFormat() {
    return srcFormat;
  }

  /**
   * @return the srcHeight
   */
  public int getSrcHeight() {
    return srcHeight;
  }

  /**
   * @return the srcWidth
   */
  public int getSrcWidth() {
    return srcWidth;
  }

  /**
   * @return the dstFormat
   */
  public AVPixFormat getDstFormat() {
    return dstFormat;
  }

  /**
   * @return the dstHeight
   */
  public int getDstHeight() {
    return dstHeight;
  }

  /**
   * @return the dstWidth
   */
  public int getDstWidth() {
    return dstWidth;
  }

  /**
   * @return the scalingMethod
   */
  public SWSInterpolation getScalingMethod() {
    return scalingMethod;
  }

  /**
   * @return the context
   */
  public SwsContext getContext() {
    return context;
  }
}
