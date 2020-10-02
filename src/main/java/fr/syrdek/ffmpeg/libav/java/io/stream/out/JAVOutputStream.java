/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.io.stream.out;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.FFmpegException;
import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVOutputContainer;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVPacket;
import fr.syrdek.ffmpeg.libav.java.io.stream.in.JAVInputStream;

/**
 * Représente un flux sortant au sein d'un conteneur (par exemple audio ou video).
 *
 * @author Syrdek
 */
public class JAVOutputStream implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(JAVOutputStream.class);

  // Informations sur le format englobant.
  protected final JAVOutputContainer container;

  // Informations sur le flux.
  protected final AVStream avstream;

  /**
   * Construit un flux sortant.
   *
   * @param container
   *          Le conteneur parent.
   * @param avstream
   *          Le flux brut libav.
   */
  public JAVOutputStream(final JAVOutputContainer container, final AVStream avstream) {
    this.container = container;
    this.avstream = avstream;
  }

  /**
   * @param name
   *          Le nom du codec a utiliser.
   * @throws FFmpegException
   *           Si le codec donné n'existe pas ou n'est pas compatible avec le type de flux.
   */
  public void setCodec(String name) {
    final AVCodec codecByName = avcodec.avcodec_find_encoder_by_name(name);

    if (codecByName == null) {
      throw new FFmpegException("Le codec '" + name + "' est inconnu.");
    }

    setCodec(codecByName);
  }

  /**
   * @param id
   *          L'id du codec a utiliser.
   * @throws FFmpegException
   *           Si le codec donné n'existe pas ou n'est pas compatible avec le type de flux.
   */
  public void setCodec(int id) {
    final AVCodec codecById = avcodec.avcodec_find_encoder(id);

    if (codecById == null) {
      throw new FFmpegException("Le codec d'id '" + id + "' est inconnu.");
    }

    setCodec(codecById);
  }

  /**
   * @param codec
   *          Le codec a utiliser.
   * @throws FFmpegException
   *           Si le codec donné n'est pas compatible avec le type de flux.
   */
  public void setCodec(final AVCodec codec) {

    // TODO
  }

  public void copyCodecParams(JAVInputStream stream) {
    checkAndThrow(avcodec.avcodec_parameters_copy(avstream.codecpar(), stream.getCodecParams()));
    // Efface le tag du codec qui est lié à son format d'entrée.
    avstream.codecpar().codec_tag(0);
  }

  /**
   * @return Le media traité par ce flux.
   */
  public Media getMedia() {
    return Media.UNKNOWN;
  }

  /**
   * Ecrit un paquet de donnée encodé.
   */
  public void writeEncodedPacket(JAVPacket packet) {
    final AVPacket avpacket = packet.getPacket();
    avpacket.stream_index(avstream.index());

    final JAVInputStream origin = packet.getOrigin();
    if (origin != null) {
      final AVRational originTimeBase = origin.getAvstream().time_base();
      // Recalcule les PTS (presentation timestamp), DTS (decoding timestamp), et la durée de l'image en fonction de la
      // nouvelle base de temps du conteneur.
      // Voir http://dranger.com/ffmpeg/tutorial05.html pour plus d'explications.
      avcodec.av_packet_rescale_ts(avpacket, originTimeBase, avstream.time_base());
    }

    container.writeInterleaved(packet);
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
      b.append("id=").append(params.codec_id()).append(", ");
    }
    // supprime la dernière virgule.
    if (b.length() > 0) {
      b.delete(b.length() - 2, b.length());
    }

    b.insert(0, "Flux sortant [");
    b.append("]");
    return b.toString();
  }

  @Override
  public void finalize() throws Exception {
    close();
  }

  @Override
  public void close() {
  }
}
