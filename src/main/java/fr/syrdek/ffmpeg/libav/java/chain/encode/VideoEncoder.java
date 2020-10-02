/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.chain.encode;

import org.bytedeco.ffmpeg.avutil.AVDictionary;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.chain.Muxer;
import fr.syrdek.ffmpeg.libav.java.io.stream.VideoParameters;

/**
 * Permet d'encoder un flux vidéo.
 *
 * @author t0087865
 */
public class VideoEncoder extends Encoder implements AutoCloseable {

  private final VideoParameters params;

  /**
   * Construit un encodeur vidéo.
   *
   * @param params
   *          Les paramètres d'encodage.
   * @param container
   *          Le conteneur dans lequel sera injecté le flux encodé.
   * @param opts
   *          Les options d'encodage.
   */
  public VideoEncoder(final VideoParameters params, final Muxer container, final AVDictionary opts) {
    super(params, container, opts);
    this.params = params;
  }

  /*
   * (non-Javadoc)
   *
   * @see fr.syrdek.ffmpeg.libav.java.chain.encode.Encoder#getMedia()
   */
  @Override
  public Media getMedia() {
    return Media.VIDEO;
  }

  /**
   * @return Les paramètres vidéo.
   */
  public VideoParameters getParameters() {
    return params;
  }
}
