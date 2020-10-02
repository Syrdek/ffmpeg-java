/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.chain.encode;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;
import static fr.syrdek.ffmpeg.libav.java.TimeUtils.timestampToHms;
import static fr.syrdek.ffmpeg.libav.java.TimeUtils.timestampToString;

import java.io.Closeable;
import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.TimeUtils;
import fr.syrdek.ffmpeg.libav.java.chain.BaseProducer;
import fr.syrdek.ffmpeg.libav.java.chain.Muxer;
import fr.syrdek.ffmpeg.libav.java.io.AVEncodingCompliance;
import fr.syrdek.ffmpeg.libav.java.io.stream.CodecContextParameters;

/**
 * Permet d'encoder un flux.
 *
 * @author t0087865
 */
public abstract class Encoder extends BaseProducer<AVPacket> implements Consumer<AVFrame>, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(Encoder.class);

  protected final Muxer container;
  protected final AVCodecContext codecCtx;
  protected final AVRational timebase;
  protected final AVStream stream;
  protected final AVCodec codec;

  protected final AVPacket packet;

  /**
   * Construit l'encodeur.
   *
   * @param params
   *          Les paramètres d'encodage.
   * @param container
   *          Le conteneur dans lequel déclarer le flux à encoder.
   * @param opts
   *          Les options d'encodage.
   */
  protected Encoder(final CodecContextParameters params, final Muxer container,
      final AVDictionary opts) {
    super();

    this.container = container;

    LOG.debug("Construction de l'encodeur {} avec les parametres {}.", getMedia(), params);

    // Paquet qui sera rempli pour l'encodage.
    packet = new AVPacket();

    // Construit le codec.
    codec = params.getNativeCodec();

    // Construit le contexte d'encodage.
    codecCtx = checkAllocation(avcodec.avcodec_alloc_context3(codec));

    // Copie les paramètres dans le contexte d'encodage.
    params.pushTo(codecCtx);

    // Conserve une copie de la timebase car l'originale peut être modifiée par ffmpeg.
    timebase = TimeUtils.copy(codecCtx.time_base()).den(1000);

    // Active les fonctionnalités expérimentales (encodage vorbis / theora).
    codecCtx.strict_std_compliance(AVEncodingCompliance.EXPERIMENTAL.value());

    // Déclare le flux au conteneur de destination.
    stream = container.addStream(codec, timebase);

    // L'unité de temps utilisée dans le flux doit être celle du codec.
    // stream.time_base(codecCtx.time_base());
    CodecContextParameters.computeTimeBase(1000, stream.time_base());

    // Construit le codec.
    checkAllocation(avcodec.avcodec_open2(codecCtx, codec, opts),
        "Echec d'ouverture du codec {}", codec.long_name().getString());

    // Copie les paramètres du flux dans le contexte d'encodage.
    checkAndThrow(avcodec.avcodec_parameters_from_context(stream.codecpar(), codecCtx));

    // TODO DEBUG
    if (LOG.isInfoEnabled()) {
      LOG.info("Encoder {} [TB: {}, codec TB: {}]", getMedia(),
          TimeUtils.toString(stream.time_base()),
          TimeUtils.toString(codecCtx.time_base()));
    }
  }

  /**
   * @return Le type de média géré par ce décodeur.
   */
  public abstract Media getMedia();

  /**
   * Encode la frame donnée.
   *
   * @param La
   *          frame à encoder.
   */
  @Override
  public void accept(final AVFrame frame) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Encodage d'une frame {} [{}] (DTS: {} [{}], PTS: {} [{}]).",
          getMedia().name(), codec.name().getString(),
          timestampToString(frame.pkt_dts()), timestampToHms(frame.pkt_dts(), stream.time_base()),
          timestampToString(frame.pts()), timestampToHms(frame.pts(), stream.time_base()));
    }

    // Evite que des données résiduelles ne perturbent l'encodeur.
    packet.data(null);
    packet.size(0);
    avcodec.av_init_packet(packet);

    checkAndThrow(avcodec.avcodec_send_frame(codecCtx, frame));

    int ret = 0;
    do {
      ret = avcodec.avcodec_receive_packet(codecCtx, packet);

      if (ret != avutil.AVERROR_EOF && ret != avutil.AVERROR_EAGAIN()) {
        // Vérifie qu'il n'y a pas eu d'erreur associée au code.
        checkAndThrow(ret);

        packet.stream_index(stream.index());

        // Envoie le paquet aux consommateurs.
        publish(packet);
      }
      // Ici, les seuls codes erreur possible sont :
      // - Soit EOF : Toute la frame donnée en entrée a été traitée.
      // - Soit EAGAIN : La frame Ne contient pas assez de données pour générer un paquet encodé complet.
      // Dans ces 2 cas, il faut attendre la prochaine frame pour générer un nouveau paquet.
    } while (ret >= 0);

    avcodec.av_packet_unref(packet);
  }

  @Override
  public void close() {
    packet.close();
    codec.close();
    stream.close();
    timebase.close();
    codecCtx.time_base().close();
    avcodec.avcodec_free_context(codecCtx);
  }

  /**
   * @return the container
   */
  public Muxer getContainer() {
    return container;
  }

  /**
   * @return the codecCtx
   */
  public AVCodecContext getCodecCtx() {
    return codecCtx;
  }

  /**
   * @return the stream
   */
  public AVStream getStream() {
    return stream;
  }

  /**
   * @return the codec
   */
  public AVCodec getCodec() {
    return codec;
  }
}
