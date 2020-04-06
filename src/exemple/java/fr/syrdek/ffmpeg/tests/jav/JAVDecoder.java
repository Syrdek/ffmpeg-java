package fr.syrdek.ffmpeg.tests.jav;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacpp.avutil.AVRational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;
import fr.syrdek.ffmpeg.libav.java.io.stream.in.JAVInputStream;
import fr.syrdek.ffmpeg.tests.Utils;

/**
 * 
 * @author Syrdek
 */
public class JAVDecoder {
  private static final Logger LOG = LoggerFactory.getLogger(JAVDecoder.class);

  static {
    // S'assure que les libs natives soient bien chargÃ©es.
    FFmpegNatives.ensureLoaded();
  }

  public static void main(String[] args) throws Exception {
    Utils.cleanup();
    try (final InputStream in = new FileInputStream("samples/audio-expected.mkv")) {
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
    final int[] nbpkt = new int[] { 0 };
    final int[] nbframe = new int[] { 0 };
    final long[] oldPts = new long[] { 0 };
    try (final JAVInputContainer container = new JAVInputContainer.Builder().build(in)) {

      container.readFully(packet -> {
        nbpkt[0]++;
        final JAVInputStream origin = packet.getOrigin();
        final AVPacket avpkt = packet.getPacket();
        final AVStream avstream = origin.getAvstream();
        final AVRational timebase = avstream.time_base();
        LOG.info("media={}, packet={}, frame={}, timebase={}/{}, duration={}, PTS={}, DTS={}", origin.getMedia(),
            nbpkt[0], nbframe[0], timebase.num(), timebase.den(), avpkt.duration(), avpkt.pts(), avpkt.dts());
        
        origin.decode(packet, frame -> {
          nbframe[0]++;
          final AVFrame avframe = frame.getFrame();
          LOG.info("media={}, packet={}, frame={}, timebase={}/{}, nbSample={}, duration={}, PTS={}, DTS={}",
              origin.getMedia(), nbpkt[0], nbframe[0], timebase.num(), timebase.den(), avframe.nb_samples(),
              avpkt.duration(), avpkt.pts(), avpkt.dts(), avpkt.pts() - oldPts[0]);
          oldPts[0] = avpkt.pts();
        });
      });
    }
  }
}
