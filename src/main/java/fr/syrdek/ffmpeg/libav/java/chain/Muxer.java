/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.chain;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;
import static fr.syrdek.ffmpeg.libav.java.TimeUtils.timestampToHms;
import static fr.syrdek.ffmpeg.libav.java.TimeUtils.timestampToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.TimeUtils;
import fr.syrdek.ffmpeg.libav.java.io.stream.CodecContextParameters;

/**
 * Permet d'écrire un conteneur media.
 *
 * @author t0087865
 */
public class Muxer implements Consumer<AVPacket>, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(Muxer.class);

  /**
   * Représente l'état courant du media.
   *
   * @author t0087865
   */
  private enum State {
    OPEN, HAS_HEADER, HAS_DATA, HAS_TRAILER, CLOSED;
  }

  private final List<AVStream> streams = new ArrayList<>();
  private final List<AVRational> timebases = new ArrayList<>();

  private final AVFormatContext formatCtx = new AVFormatContext();
  private final AVIOContext io;
  private State state;

  /**
   * Construit un conteneur audio / video.
   *
   * @param filepath
   *          Le chemin vers le fichier a écrire.
   * @param format
   *          Le format du conteneur (par exemple "matroska", "quicktime", "flv", ...).
   */
  public Muxer(final String filepath, final String format) {
    LOG.debug("Construction du conteneur de sortie {}.", filepath);

    // Construit un contexte d'écriture.
    checkAndThrow(avformat.avformat_alloc_output_context2(formatCtx, null, format, filepath),
        "Impossible d''allouer le contexte de sortie.");

    io = new AVIOContext();
    // Ouvre le fichier de sortie.
    checkAndThrow(avformat.avio_open(io, filepath, avformat.AVIO_FLAG_WRITE),
        "Impossible d''ouvrir le flux de sortie.");
    // Affecte le flux de sortie au contexte d'écriture.
    formatCtx.pb(io);

    state = State.OPEN;
  }

  /**
   * Ajoute un flux au conteneur. Equivalent à {@link Muxer#addStream(AVCodec, AVRational)} avec timebase
   * null.
   *
   * @param streamCodec
   *          Le codec utilisé pour encoder le flux.
   * @return Le flux construit.
   */
  public AVStream addStream(final AVCodec streamCodec) {
    return addStream(streamCodec, null);
  }

  /**
   * Ajoute un flux au conteneur.
   *
   * @param streamCodec
   *          Le codec utilisé pour encoder le flux.
   * @param timebase
   *          La base de temps du flux. Peut être null.
   * @return Le flux construit.
   */
  public AVStream addStream(final AVCodec streamCodec, final AVRational timebase) {
    if (State.OPEN != state) {
      // Si les entêtes sont écrites, ajouter un nouveau flux peut causer un coredump.
      // On préfère donc l'éviter en levant une exception.
      throw new IllegalStateException(
          "Impossible de déclarer un nouveau flux car les entêtes du fichier media ont déjà été écrites.");
    }

    LOG.debug("Ajout d'un flux de type {} au conteneur.", streamCodec.long_name().getString());

    // Ajoute un flux au fichier de sortie.
    final AVStream stream = checkAllocation(avformat.avformat_new_stream(formatCtx, streamCodec),
        "Impossible de construire un flux de type {0}.", streamCodec.long_name());
    stream.index(formatCtx.nb_streams() - 1);
    stream.time_base(timebase);

    timebases.add(TimeUtils.copy(timebase));
    streams.add(stream);
    return stream;
  }

  /**
   * Ecrit les entêtes propres au format conteneur.<br>
   * L'entête déclare les différents flux. Il n'est donc dès lors plus possible d'ajouter des flux au format conteneur.
   */
  public void writeHeaders() {
    // On ne peut écrire les entêtes que si elles n'ont pas encore été écrites.
    if (State.OPEN == state) {
      LOG.debug("Ecriture des entetes du conteneur de destination.");

      checkAndThrow(avformat.avformat_write_header(formatCtx, (AVDictionary) null),
          "Impossible d''écrire les entêtes du fichier de sortie.");
      state = State.HAS_HEADER;
    }
  }

  /**
   * Ecrit la fin du format conteneur.<br>
   * Il n'est alors plus possible d'ajouter des frames sur les différents flux.
   */
  public void writeTrailer() {
    // Ne réécrit pas le trailer s'il a déjà été écrit ou si aucune donnée n'a été positionnée.
    if (State.HAS_DATA == state) {
      LOG.debug("Ecriture de la finalisation du conteneur de destination.");

      checkAndThrow(avformat.av_write_trailer(formatCtx),
          "Impossible d''écrire les entêtes du fichier de sortie.");

      state = State.HAS_TRAILER;
    } else if (State.HAS_TRAILER != state) {
      LOG.error(
          "Tentative d'écriture du trailer alors que le conteneur est en l'état {}. Ce message peut signifier la fin prématurée du flux.",
          state);
    }
  }

  @Override
  public void close() {
    // S'assure que le trailer a été écrit.
    writeTrailer();
    avformat.avio_close(io);
    avutil.av_free(formatCtx);
    timebases.stream().filter(Objects::nonNull).forEach(AVRational::close);
  }

  /**
   * @return Les drapeaux modifiant le fonctionnement du conteneur.
   */
  public int flags() {
    return formatCtx.flags();
  }

  @Override
  public void accept(final AVPacket packet) {
    if (LOG.isDebugEnabled()) {
      final AVStream stream = streams.get(packet.stream_index());
      final AVRational timebase = timebases.get(packet.stream_index());
      LOG.debug("Reception d'un paquet codec \"{}\" (Stream: {}; DTS: {} [{}], PTS: {} [{}], TB: {}).",
          CodecContextParameters.codecNameFromId(stream.codecpar().codec_id()), stream.index(),
          timestampToString(packet.dts()), timestampToHms(packet.dts(), timebase),
          timestampToString(packet.pts()), timestampToHms(packet.pts(), timebase),
          TimeUtils.toString(timebase));
    }

    // S'assure que les entête sont écrites avant
    writeHeaders();

    // Ecrit le paquet dans le conteneur.
    checkAndThrow(avformat.av_interleaved_write_frame(formatCtx, packet));

    // Indique que des données ont été positionnées.
    state = State.HAS_DATA;
  }

  /**
   * Affiche les informations sur le format.
   */
  public void dumpFormat() {
    avformat.av_dump_format(formatCtx, 0, (String) null, 1);
  }
}
