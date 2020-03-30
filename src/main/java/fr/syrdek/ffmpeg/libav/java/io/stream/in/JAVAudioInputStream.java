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
 * Un flux audio.
 * 
 * @author Syrdek
 */
public class JAVAudioInputStream extends JAVInputStream {
  /**
   * Construit un flux audio.
   * 
   * @param container
   *          Le conteneur associé.
   * @param stream
   *          Le flux audio de libav.
   */
  public JAVAudioInputStream(final JAVInputContainer container, final AVStream stream) {
    super(container, stream);
  }
  
  /**
   * @return Le media traité par ce flux.
   */
  public Media getMedia() {
    return Media.AUDIO;
  }

  /**
   * Une description du flux.
   */
  public String toString() {
    return MessageFormat.format("Flux Audio entrant [type={0}, id={1}, codec={2}, channels={3}, sample rate={4}]",
        codecParams.codec_type(),
        codec.id(),
        codec.long_name().getString(),
        codecParams.channels(),
        codecParams.sample_rate());
  }

  /**
   * Ferme les ressources du flux.
   */
  public void close() {
    super.close();
  }
}
