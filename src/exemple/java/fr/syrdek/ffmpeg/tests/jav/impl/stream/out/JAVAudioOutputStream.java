/**
 *
 */
package fr.syrdek.ffmpeg.tests.jav.impl.stream.out;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avformat.AVStream;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.stream.AudioParameters;
import fr.syrdek.ffmpeg.tests.jav.impl.container.JAVOutputContainer;

/**
 * Un flux audio.
 *
 * @author Syrdek
 */
public class JAVAudioOutputStream extends JAVOutputStream {
  /**
   * Construit un flux audio.
   *
   * @param container
   *          Le conteneur associé.
   * @param stream
   *          Le flux audio de libav.
   */
  public JAVAudioOutputStream(final JAVOutputContainer container, final AudioParameters parameters,
      final AVStream stream) {
    super(container, stream);
  }

  /**
   * @return Le media traité par ce flux.
   */
  @Override
  public Media getMedia() {
    return Media.AUDIO;
  }

  /**
   * Une description du flux.
   */
  @Override
  public String toString() {
    final AVCodecParameters params = avstream.codecpar();

    final StringBuilder b = new StringBuilder();
    if (params != null) {
      b.append("type=").append(params.codec_type()).append(", ");
      b.append("channels=").append(params.channels()).append(", ");
      b.append("sample_rate=").append(params.sample_rate()).append(", ");
      b.append("id=").append(params.codec_id()).append(", ");
    }
    // supprime la dernière virgule.
    if (b.length() > 0) {
      b.delete(b.length() - 2, b.length());
    }

    b.insert(0, "Flux Audio sortant [");
    b.append("]");
    return b.toString();
  }

  /**
   * Ferme les ressources du flux.
   */
  @Override
  public void close() {
    super.close();
  }
}
