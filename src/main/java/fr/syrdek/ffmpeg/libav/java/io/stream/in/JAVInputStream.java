/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io.stream.in;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.text.MessageFormat;
import java.util.function.Consumer;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.FFmpegException;
import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVFrame;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVPacket;

/**
 * Représente un flux entrant au sein d'un conteneur (par exemple audio ou
 * video).
 * 
 * @author Syrdek
 */
public abstract class JAVInputStream implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(JAVInputStream.class);

  // Informations sur le format englobant.
  protected final JAVInputContainer container;

  // Informations sur le flux.
  protected final AVStream avstream;

  // Informations de codage.
  protected final AVCodecParameters codecParams;
  protected final AVCodecContext codecCtx;
  protected final AVCodec codec;

  // Stockage d'un paquet de données décodées.
  protected final AVFrame frame;

  /**
   * Construit un {@link JAVInputStream} à partir d'un {@link AVStream} provenant
   * de libav.
   * 
   * @param container La conteneur du flux.
   * @param avstream  Le flux.
   * @return Le {@link JAVInputStream} construit.
   */
  public static JAVInputStream create(final JAVInputContainer container, final AVStream avstream) {
    switch (avstream.codecpar().codec_type()) {
    case avutil.AVMEDIA_TYPE_AUDIO:
      return new JAVAudioInputStream(container, avstream);
    case avutil.AVMEDIA_TYPE_VIDEO:
      return new JAVVideoInputStream(container, avstream);
    default:
      return new JAVUnknownInputStream(container, avstream);
    }
  }

  /**
   * Construit un flux.
   * 
   * @param avstream  Le flux libav.
   * @param formatCtx Le format contenant le flux.
   */
  protected JAVInputStream(final JAVInputContainer container, final AVStream avstream) {
    this.container = container;
    this.avstream = avstream;

    this.codecParams = avstream.codecpar();

    // Récupère les informations de codage.
    this.codec = avcodec.avcodec_find_decoder(codecParams.codec_id());
    this.codecCtx = checkAllocation(AVCodecContext.class, avcodec.avcodec_alloc_context3(codec));
    checkAndThrow(avcodec.avcodec_parameters_to_context(codecCtx, codecParams));
    checkAndThrow(avcodec.avcodec_open2(codecCtx, codec, (AVDictionary) null));

    this.frame = avutil.av_frame_alloc();
  }

  /**
   * @return the codecParams
   */
  public AVCodecParameters getCodecParams() {
    return codecParams;
  }

  /**
   * @return the avstream
   */
  public AVStream getAvstream() {
    return avstream;
  }

  /**
   * Lit entièrement le flux, et envoie les paquets au consumer donné, au fur et à
   * mesure de la lecture.
   * 
   * @param packetConsumer Le consumer auquel envoyer les paquets lus.
   */
  public void readFully(final Consumer<JAVPacket> packetConsumer) {
    // Préalloue un paquet qui contiendra les données encodées.
    final AVPacket packet = checkAllocation(AVPacket.class, avcodec.av_packet_alloc());
    try {
      final AVFormatContext formatCtx = container.getFormatCtx();
      // Lit un paquet de données brutes depuis le flux.
      while (avformat.av_read_frame(formatCtx, packet) >= 0) {
        // Décode le paquet lu.
        packetConsumer.accept(new JAVPacket(this, packet));
      }
      // Libère le paquet.
      avcodec.av_packet_unref(packet);
    } finally {
      // Nettoie la mémoire.
      avcodec.av_packet_free(packet);
    }
  }

  /**
   * Decode entièrement le flux et le décode. Envoie les paquets décodés au
   * consumer donné.
   * 
   * @param frameConsumer Le consumer recevant les paquets décodés.
   */
  public void decodeFully(final Consumer<JAVFrame> frameConsumer) {
    readFully(t -> this.decode(t, frameConsumer));
  }
  
  /**
   * Décode un paquet de données, et renvoie les frames issues de ce paquet au
   * consumer donné.<br>
   * Un paquet à décoder peut contenir de 0 à plusieurs frames. Le cas le plus
   * fréquent étant une frame de données.
   * 
   * @param packet        Le paquet à décoder.
   * @param frameConsumer Le consumer qui sera appelé pour chaque frame décodée.
   */
  public void decode(final JAVPacket packet, final Consumer<JAVFrame> frameConsumer) {
    // Envoie le paquet récupéré au codec pour décodage.    
    int ret = FFmpegException.checkAndThrow(avcodec.avcodec_send_packet(codecCtx, packet.getPacket()));
    while (ret >= 0) {
      // Récupère la frame décodée par le codec.
      ret = avcodec.avcodec_receive_frame(codecCtx, frame);
      if (ret == avutil.AVERROR_EOF || ret == avutil.AVERROR_EAGAIN()) {
        // Ces erreurs indiquent la fin de traitement et sont attendues.
        break;
      }
      // Les autres codes d'erreur sont graves.
      FFmpegException.checkAndThrow(ret);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Frame : Size={}, number={}, PTS={}, DTS={}, key_frame={}", frame.pkt_size(), codecCtx.frame_number(),
            frame.pts(), frame.pkt_dts(), frame.key_frame());
      }

      frameConsumer.accept(new JAVFrame(this, frame));
      
      // Libère la frame.
      avutil.av_frame_unref(frame);
    }
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
    return MessageFormat.format("Flux [type={0}, codec={1}, id={2}]", codecParams.codec_type(),
        codec.long_name().getString(), codec.id());
  }

  @Override
  public void finalize() throws Exception {
    close();
  }

  @Override
  public void close() {
    if (frame != null)
      avutil.av_frame_free(frame);
    if (codecCtx != null)
      avcodec.avcodec_free_context(codecCtx);
  }
}
