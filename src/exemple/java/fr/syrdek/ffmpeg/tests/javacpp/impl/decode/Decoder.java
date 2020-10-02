/**
 *
 */
package fr.syrdek.ffmpeg.tests.javacpp.impl.decode;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;
import static fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils.timestampToHms;
import static fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils.timestampToString;

import java.io.Closeable;
import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.tests.javacpp.impl.BaseProducer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils;

/**
 * Permet de décoder un flux.
 *
 * @author t0087865
 */
public abstract class Decoder extends BaseProducer<AVFrame> implements Consumer<AVPacket>, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(Decoder.class);

  /**
   * Ouvre le décodeur du flux donné.
   *
   * @param inFmtCtx
   *          Le format de fichier contenant le flux.
   * @param index
   *          Le numéro du flux à ouvrir.
   * @param options
   *          Options a utiliser pour la construction du décodeur. Peut être <code>null</code>.
   * @return Le contexte de décodage à utiliser pour lire le flux. <code>null</code> si aucun flux du type demandé n'a
   *         été trouvé.
   */
  public static Decoder openStreamDecoder(final AVFormatContext inFmtCtx, final int index,
      final AVDictionary options) {
    /// Ouvre le flux trouvé.
    final AVStream stream = inFmtCtx.streams(index);
    final int codec_id = stream.codecpar().codec_id();
    // Charge le décodeur du flux.
    final AVCodec decoder = checkAllocation(avcodec.avcodec_find_decoder(codec_id),
        "Impossible de trouver le décodeur d'id {0}", codec_id);
    final AVCodecContext codecCtx = avcodec.avcodec_alloc_context3(decoder);
    // Donne au codec les paramètres permettant de décoder le flux.
    checkAndThrow(avcodec.avcodec_parameters_to_context(codecCtx, stream.codecpar()));

    // Ouvre le décodeur.
    checkAndThrow(avcodec.avcodec_open2(codecCtx, decoder, options),
        "Impossible d'ouvrir le codec {0} d'id {1}.",
        decoder.name(), codec_id);

    final Media type = Media.of(stream.codecpar().codec_type());

    if (LOG.isInfoEnabled()) {
      LOG.info("Decoder {} [stream TB: {}, codec TB: {}]", type,
          TimeUtils.toString(stream.time_base()),
          TimeUtils.toString(codecCtx.time_base()));
    }

    switch (type) {
    case VIDEO:
      return new VideoDecoder(inFmtCtx, stream, decoder, codecCtx);
    case AUDIO:
      return new AudioDecoder(inFmtCtx, stream, decoder, codecCtx);
    default:
      LOG.error("Aucun décodeur n'est implémenté pour gérer les flux de type {}", type);
      break;
    }
    return null;
  }

  /**
   * Ouvre le décodeur du flux donné.
   *
   * @param inFmtCtx
   *          Le format de fichier contenant le flux.
   * @param type
   *          Le type de flux à ouvrir.
   * @param options
   *          Options a utiliser pour la construction du décodeur. Peut être <code>null</code>.
   * @return Le contexte de décodage à utiliser pour lire le flux. <code>null</code> si aucun flux du type demandé n'a
   *         été trouvé.
   */
  public static Decoder openStreamDecoder(final AVFormatContext inFmtCtx, final Media type,
      final AVDictionary options) {
    // Récupère le flux demandé.
    int streamIdx = avformat.av_find_best_stream(inFmtCtx, type.value(), -1, -1, (AVCodec) null, 0);
    if (streamIdx < 0) {
      // Il n'y a pas de flux de ce type.
      LOG.warn("Aucun flux {} n'est présent dans le fichier d'entrée", type);
    }
    return openStreamDecoder(inFmtCtx, streamIdx, options);
  }

  /**
   * Ouvre le décodeur du flux donné.
   *
   * @param inFmtCtx
   *          Le format de fichier contenant le flux.
   * @param type
   *          Le type de flux à ouvrir.
   * @return Le contexte de décodage à utiliser pour lire le flux. <code>null</code> si aucun flux du type demandé n'a
   *         été trouvé.
   */
  public static Decoder openStreamDecoder(final AVFormatContext inFmtCtx, final Media type) {
    return openStreamDecoder(inFmtCtx, type);
  }

  // Est notifié chaque fois qu'une frame est décodée.
  protected Consumer<AVFrame> frameConsumer;

  protected final AVFormatContext formatCtx;
  protected final AVCodecContext codecCtx;
  protected final AVStream stream;
  protected final AVCodec codec;
  protected final AVFrame frame;

  /**
   *
   * @param formatCtx
   *          Format duquel est extrait le flux a décoder.
   * @param stream
   *          Le flux à décoder.
   * @param codec
   *          Le codec à utiliser pour décoder le flux.
   * @param codecCtx
   *          Le contexte de décodage du flux.
   */
  protected Decoder(final AVFormatContext formatCtx, final AVStream stream, final AVCodec codec,
      final AVCodecContext codecCtx) {
    frame = checkAllocation(avutil.av_frame_alloc(), "Impossible d'allouer la frame de décodage {0}", getMedia());
    this.formatCtx = formatCtx;
    this.codecCtx = codecCtx;
    this.stream = stream;
    this.codec = codec;
  }

  /**
   * @return Le type de média géré par ce décodeur.
   */
  public abstract Media getMedia();

  /**
   * Décode le paquet donné.
   */

  @Override
  public void accept(final AVPacket packet) {
    if (!isAddressedBy(packet)) {
      // Ne doit pas accepter de paquet qui n'est pas issu du stream a décoder.
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Décodage d'un paquet {} [{}] (DTS: {} [{}], PTS: {} [{}]).", getMedia().name(),
          codec.name().getString(),
          timestampToString(packet.dts()), timestampToHms(packet.dts(), stream.time_base()),
          timestampToString(packet.pts()), timestampToHms(packet.pts(), stream.time_base()));
    }

    checkAndThrow(avcodec.avcodec_send_packet(codecCtx, packet),
        "Impossible d'envoyer le paquet au décodage {0} (codec {1})", getMedia().name(), codec.name().getString());

    int ret;
    do {
      ret = avcodec.avcodec_receive_frame(codecCtx, frame);
      if (ret != avutil.AVERROR_EOF && ret != avutil.AVERROR_EAGAIN()) {
        // Vérifie qu'il n'y a pas eu d'erreur associée au code.
        checkAndThrow(ret, "Erreur inattendue lors du décodage du paquet {0} (codec {1})", getMedia().name(),
            codec.name().toString());

        // Envoie la frame.
        publish(frame);
      }
      // Ici, les seuls codes erreur possible sont :
      // - Soit EOF : Toutes les données ont été traitées.
      // - Soit EAGAIN : Les données restantes ne sont pas suffisantes pour décoder une nouvelle frame.
      // Dans ces 2 cas, il faut attendre le prochain paquet pour avoir de nouvelles frames.
    } while (ret >= 0);
  }

  /**
   * Libère les ressources.
   */
  @Override
  public void close() {
    avcodec.avcodec_free_context(codecCtx);
    avutil.av_frame_free(frame);
  }

  /**
   * @param packet
   *          Un paquet à tester.
   * @return <code>true</code> si le paquet est issu du stream géré par ce décodeur.<code>false</code> sinon.
   */
  public boolean isAddressedBy(final AVPacket packet) {
    return packet.stream_index() == getStreamIndex();
  }

  /**
   * @return L'index du flux décodé.
   */
  public int getStreamIndex() {
    return stream.index();
  }

  /**
   * @return the formatCtx
   */
  public AVFormatContext getFormatCtx() {
    return formatCtx;
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
