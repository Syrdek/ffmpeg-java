/**
 *
 */
package fr.syrdek.ffmpeg.tests.javacpp.impl.encode;

import org.bytedeco.ffmpeg.avutil.AVDictionary;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.stream.AudioParameters;
import fr.syrdek.ffmpeg.tests.javacpp.impl.Muxer;

/**
 * Permet d'encoder un flux audio.
 *
 * @author t0087865
 */
public class AudioEncoder extends Encoder implements AutoCloseable {

  private final AudioParameters params;

  /**
   * Construit un encodeur audio.
   *
   * @param params
   *          Les paramètres d'encodage.
   * @param container
   *          Le conteneur dans lequel sera injecté le flux encodé.
   * @param opts
   *          Les options d'encodage.
   */
  public AudioEncoder(final AudioParameters params, final Muxer container, final AVDictionary opts) {
    super(params, container, opts);
    this.params = params;
  }

  /*
   * (non-Javadoc)
   *
   * @see fr.syrdek.ffmpeg.tests.javacpp.impl.encode.Encoder#getMedia()
   */
  @Override
  public Media getMedia() {
    return Media.AUDIO;
  }

  /**
   * @return Les paramètres audio.
   */
  public AudioParameters getParameters() {
    return params;
  }
}
