/**
 *
 */
package fr.syrdek.ffmpeg.tests.jav.impl.stream.in;

import java.text.MessageFormat;

import org.bytedeco.ffmpeg.avformat.AVStream;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.tests.jav.impl.container.JAVInputContainer;

/**
 * Un flux inconnu (non audio ou video).
 *
 * @author Syrdek
 */
public class JAVUnknownInputStream extends JAVInputStream {
  /**
   * Construit un flux.
   *
   * @param stream
   *          Le flux de libav.
   * @param formatCtx
   *          Le format contenant le flux.
   */
  public JAVUnknownInputStream(final JAVInputContainer container, final AVStream stream) {
    super(container, stream);
  }

  /**
   * @return Le media traité par ce flux.
   */
  @Override
  public Media getMedia() {
    return Media.UNKNOWN;
  }

  /**
   * Une description du flux.
   */
  @Override
  public String toString() {
    return MessageFormat.format("Flux entrant [type={0}, codec={1}, id={2}]", codecParams.codec_type(),
        codec.long_name().getString(), codec.id());
  }

  /**
   * Ferme les ressources du flux.
   */
  @Override
  public void close() {
    super.close();
  }
}
