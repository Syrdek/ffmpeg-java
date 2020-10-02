package fr.syrdek.ffmpeg.libav.java.io.container;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.CFlag;
import fr.syrdek.ffmpeg.libav.java.FFmpegException;
import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.AVFormatFlag;
import fr.syrdek.ffmpeg.libav.java.io.stream.AudioParameters;
import fr.syrdek.ffmpeg.libav.java.io.stream.out.JAVAudioOutputStream;
import fr.syrdek.ffmpeg.libav.java.io.stream.out.JAVOutputStream;

/**
 * Permet d'écrire dans un conteneur Audio/Video.<br>
 * Cet objet doit être construit en passant par un {@link JAVOutputContainer.Builder}, qui permet de gérer les
 * paramètres obligatoires et optionels.
 *
 * @author Syrdek
 */
public class JAVOutputContainer implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(JAVOutputContainer.class);
  private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

  static {
    // S'assure que les libs natives soient bien chargées.
    FFmpegNatives.ensureLoaded();
  }

  /**
   * Gère les différents paramètres de construction d'un {@link JAVInputContainer}.
   *
   * @author Syrdek
   */
  public static class Builder {
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private AVOutputFormat format;

    /**
     * Utilise le format ayant le nom donné en paramètre.
     *
     * @param name
     *          Le nom du format à rechercher.
     * @throws FFmpegException
     *           Si le format donné est inconnu.
     */
    public Builder withFormatName(final String name) {
      format = avformat.av_guess_format(name, null, null);
      if (format == null) {
        throw new FFmpegException("Format " + name + " inconnu");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Format choisi a partir du nom {} : {} ({}) mime={}",
            name,
            format.long_name().getString(),
            format.name().getString(),
            format.mime_type().getString());
      }
      return this;
    }

    /**
     * @param name
     *          Le nom du format à rechercher.
     * @throws FFmpegException
     *           Si le format donné est inconnu.
     */
    public Builder withFormatFilename(final String name) {
      format = avformat.av_guess_format(null, name, null);
      if (format == null) {
        throw new FFmpegException("Format associé au fichier " + name + " inconnu");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Format choisi a partir du nom de fichier {} : {} ({}) mime={}",
            name,
            format.long_name().getString(),
            format.name().getString(),
            format.mime_type().getString());
      }
      return this;
    }

    /**
     * @param mime
     *          Le nom du format à rechercher.
     * @throws FFmpegException
     *           Si le format donné est inconnu.
     */
    public Builder withFormatMimeType(final String mime) {
      format = avformat.av_guess_format(null, null, mime);
      if (format == null) {
        throw new FFmpegException("Format associé au type mime " + mime + " inconnu");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Format choisi a partir du type mime {} : {} ({}) mime={}",
            mime,
            format.long_name().getString(),
            format.name().getString(),
            format.mime_type().getString());
      }
      return this;
    }

    /**
     * @param format
     *          Le format a utiliser.
     */
    public Builder withAvFormat(final AVOutputFormat format) {
      this.format = format;
      return this;
    }

    /**
     * @param bufferSize
     *          La taille du buffer de lecture.
     */
    public Builder withBufferSize(final int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    /**
     * @return Le {@link JAVInputContainer} construit.
     */
    public JAVOutputContainer build(final OutputStream out) {
      return new JAVOutputContainer(out, format, bufferSize);
    }
  }

  /**
   * Construit un pointeur de fonction de lecture de flux.<br>
   * <b>Attention :</b> Il est préférable d'utiliser un buffer de la même taille que celui utilisé par libav, car le
   * remplissage est extrèmement lent sinon.
   *
   * @param in
   *          Le flux à lire.
   * @return Le pointeur de fonction permettant de remplir un buffer de lecture.
   */
  public static final Write_packet_Pointer_BytePointer_int newAvIoWriter(final OutputStream out, int bufferSize) {
    return new Write_packet_Pointer_BytePointer_int() {
      private final byte[] dataBuffer = new byte[bufferSize];

      /**
       * Remplit le buffer avec les <code>len</code> prochains octets.<br>
       *
       * @return Le nombre d'octets lus. <code>avutil.AVERROR_EOF</code> si le fichier est terminé.
       *         <code>avutil.AVERROR_EIO</code> Si une erreur de lecture est survenue.
       */
      @Override
      public int call(final Pointer opaque, final BytePointer buffer, int len) {
        try {
          int written = 0;
          // Tant qu'il reste des données à envoyer.
          while (written < len) {
            int toWrite = Math.min(len - written, dataBuffer.length);

            // Récupère les données depuis le buffer de libav
            buffer.get(dataBuffer, written, toWrite);

            // Envoie les données dans le flux de sortie.
            out.write(dataBuffer, 0, toWrite);
            written += toWrite;
          }
          return written;
        } catch (Exception e) {
          LOG.error("Echec lors de l'ecriture du flux", e);
          // Informe FFMPEG que la lecture a échoué.
          return avutil.AVERROR_EIO();
        }
      }
    };
  }

  private final AVFormatContext formatCtx;
  private final BytePointer streamPtr;
  private final AVIOContext ioCtx;

  private final OutputStream output;

  private boolean headerWritten = false;

  private List<JAVOutputStream> streams = null;

  /**
   * Construit un container à partir du flux donné.
   *
   * @param input
   *          Le flux contenant les données audio/video à décoder.
   * @param format
   *          Le format du conteneur.
   * @param bufferSize
   *          La taille allouée au buffer de lecture.
   */
  JAVOutputContainer(final OutputStream output, final AVOutputFormat format, final int bufferSize) {
    this.output = output;

    // Définition du buffer, partagé avec FFMPEG natif.
    streamPtr = new BytePointer(avutil.av_malloc(bufferSize));
    streamPtr.capacity(bufferSize);

    // Préparation du contexte.
    ioCtx = checkAllocation(AVIOContext.class, avformat.avio_alloc_context(
        streamPtr,
        bufferSize,
        1,
        null,
        null,
        newAvIoWriter(output, bufferSize),
        null));

    // Non seekable, non writable.
    ioCtx.seekable(0);
    ioCtx.max_packet_size(bufferSize);

    // Préparation du format.
    formatCtx = checkAllocation(AVFormatContext.class, avformat.avformat_alloc_context());
    formatCtx.flags(CFlag.plus(formatCtx.flags(), AVFormatFlag.CUSTOM_IO));

    checkAndThrow(avformat.avformat_alloc_output_context2(formatCtx, format, (String) null, null));
    LOG.debug("Flux d'écriture libav ouvert");

    formatCtx.pb(ioCtx);
    streams = new ArrayList<>();
  }

  /**
   * Ecrit les entêtes du fichier. Attention, tous les flux doivent avoir été ajoutés au conteneur avant d'écrire les
   * entêtes.
   */
  public void writeHeaders() {
    if (streams.isEmpty()) {
      throw new IllegalStateException("Aucun flux n'est présent lors de l'écriture des entêtes.");
    }

    LOG.debug("Ecriture des entêtes du conteneur");
    checkAndThrow(avformat.avformat_write_header(formatCtx, (AVDictionary) null));
    headerWritten = true;
  }

  /**
   * Ecrit un paquet de donnée.
   *
   * @param packet
   *          Le paquet à écrire.
   */
  public void writeInterleaved(JAVPacket packet) {
    checkAndThrow(avformat.av_interleaved_write_frame(formatCtx, packet.getPacket()));
  }

  /**
   * Ecrit le trailer.
   */
  public void writeTrailer() {
    LOG.debug("Ecriture des pieds du conteneur");
    checkAndThrow(avformat.av_write_trailer(formatCtx));
  }

  /**
   * Ajoute un flux de données au conteneur.
   *
   * @return Le flux audio créé.
   */
  public JAVOutputStream addStream() {
    if (headerWritten) {
      throw new IllegalStateException("Les flux ne peuvent plus être ajoutés après que les entêtes aient été écrites");
    }
    LOG.debug("Ajout d'un flux au conteneur.");
    final AVStream avstream = checkAllocation(avformat.avformat_new_stream(formatCtx, null));
    final JAVOutputStream javstream = new JAVOutputStream(this, avstream);
    streams.add(javstream);
    return javstream;
  }

  /**
   * Ajoute un flux de données audio au conteneur.
   *
   * @return Le flux audio créé.
   */
  public JAVAudioOutputStream addStream(final AudioParameters parameters) {
    if (headerWritten) {
      throw new IllegalStateException("Les flux ne peuvent plus être ajoutés après que les entêtes aient été écrites");
    }
    LOG.debug("Ajout d'un flux audio au conteneur.");
    final AVStream avstream = checkAllocation(avformat.avformat_new_stream(formatCtx, null));
    final JAVAudioOutputStream javstream = new JAVAudioOutputStream(this, parameters, avstream);
    streams.add(javstream);
    return javstream;
  }

  /**
   * @return the streams
   */
  public List<JAVOutputStream> getStreams() {
    return Collections.unmodifiableList(streams);
  }

  @Override
  public void finalize() throws IOException {
    close();
  }

  @Override
  public void close() throws IOException {
    if (streams != null) {
      streams.forEach(JAVOutputStream::close);
    }
    avformat.avformat_free_context(formatCtx);
    avformat.avio_context_free(ioCtx);

    avutil.av_free(streamPtr);
    streamPtr.deallocate();

    output.close();
  }
}
