/**
 *
 */
package fr.syrdek.ffmpeg.tests.javacpp.impl.process;

import java.io.Closeable;
import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.tests.javacpp.impl.BaseProducer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils;
import fr.syrdek.ffmpeg.tests.javacpp.impl.decode.VideoDecoder;
import fr.syrdek.ffmpeg.tests.javacpp.impl.encode.VideoEncoder;

/**
 * Permet de recalculer les DTS / PTS d'un paquet suite à encodage.<br>
 * - PTS (presentation timestamp) : Définit quand doit être jouée la frame (vidéo) ou le sample (audio) lors de la
 * lecture du flux.<br>
 * - DTS (decoding timestamp) : Définit quand doit être décodée une frame ou un sample.<br>
 * Les modifications sont réalisées directement dans les frames fournies en paramètre.
 *
 * @author t0087865
 */
public class TimestampScaler extends BaseProducer<AVPacket> implements Consumer<AVPacket>, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(TimestampScaler.class);

  private final AVRational sourceTimeBase;
  private final AVRational targetTimeBase;
  private boolean scalingIsNeeded = false;

  /**
   * Détermine si la mise à l'échelle des timestamps est nécessaire.
   *
   * @param sourceTimeBase
   *          La timebase source.
   * @param targetTimeBase
   *          La timebase de destination.
   * @return <code>true</code> si les timebases diffèrent, <code>false</code> sinon.
   */
  public static boolean isScalingNeeded(final AVRational sourceTimeBase, final AVRational targetTimeBase) {
    return sourceTimeBase.den() != targetTimeBase.den() || sourceTimeBase.num() != targetTimeBase.num();
  }

  /**
   * Détermine si la mise à l'échelle des timestamps est nécessaire.
   *
   * @param source
   *          Le décodeur duquel est extraite la base de temps source.
   * @param target
   *          L'encodeur duquel est extraite la base de temps cible.
   */
  public TimestampScaler(final AVRational source, final AVRational target) {
    sourceTimeBase = TimeUtils.copy(source);
    targetTimeBase = TimeUtils.copy(target);
    scalingIsNeeded = isScalingNeeded(source, target);

  }

  /**
   * Détermine si la mise à l'échelle des timestamps est nécessaire.
   *
   * @param decoder
   *          Le décodeur duquel est extraite la base de temps source.
   * @param encoder
   *          L'encodeur duquel est extraite la base de temps cible.
   */
  public TimestampScaler(final VideoDecoder decoder, final VideoEncoder encoder) {
    this(decoder.getStream().time_base(), encoder.getStream().time_base());
  }

  @Override
  public void accept(final AVPacket packet) {
    if (scalingIsNeeded && packet.pts() > 0) {
      long oldDts = packet.dts();
      long oldPts = packet.pts();

      // Recalcule les PTS (presentation timestamp), DTS (decoding timestamp), et la durée de l'image en fonction de
      // la nouvelle base de temps du conteneur.
      // Voir http://dranger.com/ffmpeg/tutorial05.html pour plus d'explications.
      avcodec.av_packet_rescale_ts(packet, sourceTimeBase, targetTimeBase);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Nouveaux timestamps du paquet calculés (DTS: [{}] {}->{}, PTS: [{}] {}->{} ).",
            TimeUtils.timestampToHms(packet.dts(), targetTimeBase), oldDts, packet.dts(),
            TimeUtils.timestampToHms(packet.pts(), targetTimeBase), oldPts, packet.pts());
      }

      publish(packet);
    }
  }

  @Override
  public void close() {
    sourceTimeBase.close();
    targetTimeBase.close();
  }
}
