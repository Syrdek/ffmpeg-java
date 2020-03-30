package fr.syrdek.ffmpeg.tests.jav;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;
import fr.syrdek.ffmpeg.libav.java.io.stream.in.JAVInputStream;

/**
 * 
 * @author Syrdek
 */
public class JAVDecoder {
  private static final Logger LOG = LoggerFactory.getLogger(JAVDecoder.class);

  static {
    // S'assure que les libs natives soient bien chargées.
    FFmpegNatives.ensureLoaded();
  }
  
  public static void main(String[] args) throws Exception {
    try (final InputStream in = new FileInputStream("samples/video.mp4")) {
      new JAVDecoder().printInfos(in);
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }

  /**
   *
   * @param in
   * @throws IOException
   */
  public void printInfos(final InputStream in) throws IOException {
    try (final JAVInputContainer container = new JAVInputContainer.Builder().build(in)) {
      final List<JAVInputStream> streams = container.getStreams();
      streams.forEach(JAVInputStream::decode);
    }
  }
}
