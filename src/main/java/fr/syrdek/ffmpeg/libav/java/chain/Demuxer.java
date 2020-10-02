package fr.syrdek.ffmpeg.libav.java.chain;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.chain.decode.AudioDecoder;
import fr.syrdek.ffmpeg.libav.java.chain.decode.Decoder;
import fr.syrdek.ffmpeg.libav.java.chain.decode.VideoDecoder;

/**
 * Permet de lire un conteneur media.
 *
 * @author t0087865
 */
public class Demuxer implements AutoCloseable, Producer<AVPacket> {
  private static final Logger LOG = LoggerFactory.getLogger(Demuxer.class);

  private Consumer<AVPacket> consumer;

  private final AVFormatContext formatCtx;
  private final AVPacket packet;

  /**
   *
   * @param filepath
   *          Le chemin vers le fichier a ouvrir.
   */
  public Demuxer(final String filepath) {
    formatCtx = checkAllocation(avformat.avformat_alloc_context(),
        "Impossible d''allouer le format du fichier d''entrée");
    // Ouvre le fichier d'entrée.
    checkAndThrow(avformat.avformat_open_input(formatCtx, filepath, null, null),
        "Impossible d''ouvrir le fichier d''entrée {0}", filepath);
    // Récupère les informations de format du fichier.
    checkAndThrow(avformat.avformat_find_stream_info(formatCtx, (AVDictionary) null),
        "Impossible de lire les informations de format du fichier d''entrée {0}", filepath);

    // Alloue la frame et le paquet de décodage.
    packet = checkAllocation(avcodec.av_packet_alloc(), "Impossible d''allouer le paquet de décodage.");
    avcodec.av_init_packet(packet);
  }

  /**
   * Affiche le format du fichier d'entrée.
   */
  public void dumpFormat() {
    avformat.av_dump_format(formatCtx, 0, (String) null, 0);
  }

  /**
   * Ouvre le flux donné
   *
   * @param index
   *          Le numéro de flux à ouvrir.
   * @param options
   *          Les options de décodage. Peut être <code>null</code>.
   * @return Le décodeur du flux demandé.
   */
  public Decoder openDecoder(int index, final AVDictionary options) {
    final Decoder decoder = Decoder.openStreamDecoder(formatCtx, index, options);
    sendTo(decoder);
    return decoder;
  }

  /**
   * Ouvre le flux vidéo de meilleure qualité possible.
   *
   * @param options
   *          Les options de décodage. Peut être <code>null</code>.
   * @return Le décodeur du meilleur flux vidéo contenu.
   */
  public VideoDecoder openVideoDecoder(final AVDictionary options) {
    LOG.debug("Ouverture d'un flux video");
    final Decoder decoder = Decoder.openStreamDecoder(formatCtx, Media.VIDEO, options);
    sendTo(decoder);
    return (VideoDecoder) decoder;
  }

  /**
   * Ouvre le flux audio de meilleure qualité possible.
   *
   * @param options
   *          Les options de décodage. Peut être <code>null</code>.
   * @return Le décodeur du meilleur flux audio contenu.
   */
  public AudioDecoder openAudioDecoder(final AVDictionary options) {
    LOG.debug("Ouverture d'un flux audio");
    final Decoder decoder = Decoder.openStreamDecoder(formatCtx, Media.AUDIO, options);
    sendTo(decoder);
    return (AudioDecoder) decoder;
  }

  /**
   * Decode entièrement les flux du conteneur.<br>
   * Seuls les décodeurs ouverts via {@link Demuxer#openDecoder(int, AVDictionary)},
   * {@link Demuxer#openVideoDecoder(AVDictionary)}, ou
   * {@link Demuxer#openAudioDecoder(AVDictionary)} seront traités, les autres étant ignorés.<br>
   * Les frames lues dans le conteneur seront dispatchées dans les décodeurs ouverts afin d'y être décodées. Le résultat
   * peur être récupéré en positionnant un consumer dans chacun de ces décodeurs via
   * {@link Decoder#setConsumer(java.util.function.Consumer)}.
   */
  public void read() {
    LOG.debug("Lecture du conteneur en entree");
    while (avformat.av_read_frame(formatCtx, packet) >= 0) {
      consumer.accept(packet);
    }
  }

  @Override
  public void close() {
    LOG.debug("Fermeture du conteneur en entree");
    packet.close();
    avformat.avformat_close_input(formatCtx);
  }

  @Override
  public Consumer<AVPacket> getConsumer() {
    return consumer;
  }

  @Override
  public void setConsumer(Consumer<AVPacket> consumer) {
    this.consumer = consumer;
  }
}
