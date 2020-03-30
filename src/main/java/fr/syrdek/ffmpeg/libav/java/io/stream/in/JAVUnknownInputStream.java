/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io.stream.in;

import java.text.MessageFormat;

import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVStream;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;

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
  public Media getMedia() {
    return Media.UNKNOWN;
  }

  /**
   * Une description du flux.
   */
  public String toString() {
    return MessageFormat.format("Flux entrant [type={0}, codec={1}, id={2}]", codecParams.codec_type(), codec.long_name().getString(), codec.id());
  }

  /**
   * Ferme les ressources du flux.
   */
  public void close() {
    super.close();
  }
}
