/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.io.stream.in;

import java.text.MessageFormat;

import org.bytedeco.ffmpeg.avformat.AVStream;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;

/**
 * Un flux video.
 *
 * @author Syrdek
 */
public class JAVVideoInputStream extends JAVInputStream {
  /**
   * Construit un flux video.
   *
   * @param stream
   *          Le flux video de libav.
   */
  public JAVVideoInputStream(final JAVInputContainer container, final AVStream stream) {
    super(container, stream);
  }

  /**
   * @return Le media traité par ce flux.
   */
  @Override
  public Media getMedia() {
    return Media.VIDEO;
  }

  /**
   * Une description du flux.
   */
  @Override
  public String toString() {
    return MessageFormat.format("Flux Video entrant [type={0}, id={1}, codec={2}, resolution {3}x{4}]",
        codecParams.codec_type(),
        codec.id(),
        codec.long_name().getString(),
        codecParams.width(),
        codecParams.height());
  }

  /**
   * Ferme les ressources du flux.
   */
  @Override
  public void close() {
    super.close();
  }
}
