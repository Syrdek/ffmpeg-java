/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.chain.process;

import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.chain.BaseProducer;

/**
 * Permet de générer les timestamps des paquets lorsqu'ils sont absents (par exemple quite à un resampling).
 *
 * @author t0087865
 */
public class AudioTimestamper extends BaseProducer<AVPacket> implements Consumer<AVPacket> {
  private static final Logger LOG = LoggerFactory.getLogger(AudioTimestamper.class);

  private long currentTs = 0l;

  @Override
  public void accept(final AVPacket packet) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Timestamping audio packet (duration: {}, DTS:{}, PTS:{})",
          packet.duration(), packet.dts(), packet.pts());
    }
    packet.dts(currentTs);
    packet.pts(currentTs);
    currentTs += packet.duration();
    publish(packet);
  }
}
