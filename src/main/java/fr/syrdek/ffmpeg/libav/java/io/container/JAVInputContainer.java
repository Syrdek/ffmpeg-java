package fr.syrdek.ffmpeg.libav.java.io.container;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVInputFormat;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.CFlag;
import fr.syrdek.ffmpeg.libav.java.FFmpegException;
import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.AVFormatFlag;
import fr.syrdek.ffmpeg.libav.java.io.stream.in.JAVInputStream;

/**
 * Permet de lire dans un conteneur Audio/Video.<br>
 * Cet objet doit être construit en passant par un {@link JAVInputContainer.Builder}, qui permet de gérer les paramètres obligatoires et optionels.
 * 
 * @author Syrdek
 */
public class JAVInputContainer implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(JAVInputContainer.class);
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
    private AVInputFormat format;

    /**
     * Utilise le format ayant le nom donné en paramètre.
     * 
     * @param name
     *          Le nom du format à rechercher.
     * @throws FFmpegException
     *           Si le format donné est inconnu.
     */
    public Builder withFormatName(final String name) {
      this.format = avformat.av_find_input_format(name);
      if (format == null) {
        throw new FFmpegException("Format " + name + " inconnu");
      }
      return this;
    }

    /**
     * @param format
     *          Le format a utiliser.
     */
    public Builder withAvFormat(final AVInputFormat format) {
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
     * @param in
     *          Le flux de lecture depuis lequel récupérer les données.
     * @return Le {@link JAVInputContainer} construit.
     */
    public JAVInputContainer build(final InputStream in) {
      return new JAVInputContainer(in, format, bufferSize);
    }
  }

  /**
   * Construit un pointeur de fonction de lecture de flux.
   * 
   * @param in
   *          Le flux à lire.
   * @return Le pointeur de fonction permettant de remplir un buffer de lecture.
   */
  public static final Read_packet_Pointer_BytePointer_int newAvIoReader(final InputStream in, int bufferSize) {
    return new Read_packet_Pointer_BytePointer_int() {
      private byte[] dataBuffer = new byte[bufferSize];

      /**
       * Remplit le buffer avec les <code>len</code> prochains octets.<br>
       * 
       * @return Le nombre d'octets lus.
       *         <code>avutil.AVERROR_EOF</code> si le fichier est terminé.
       *         <code>avutil.AVERROR_EIO</code> Si une erreur de lecture est survenue.
       */
      @Override
      public int call(final Pointer opaque, final BytePointer buffer, int len) {
        try {
          int read = in.read(dataBuffer, 0, Math.min(len, dataBuffer.length));
          if (read <= 0) {
            LOG.debug("Fin de fichier rencontrée lors de la lecture");
            // Informe ffmpeg que le flux est terminé.
            return avutil.AVERROR_EOF;
          }
          buffer.position(0).put(dataBuffer);

          // Restaure la limite précédente.
          return (int) read;
        } catch (IOException e) {
          LOG.error("Echec lors de la lecture du flux", e);
          // Informe FFMPEG que la lecture a échoué.
          return avutil.AVERROR_EIO();
        }
      }
    };
  }

  private final AVFormatContext formatCtx;
  private final BytePointer streamPtr;
  private final AVIOContext ioCtx;

  private final InputStream input;

  private List<JAVInputStream> streams = null;
  private final AVInputFormat format;

  // Garde en mémoire les paquets en cours de décodage.
  private AVPacket currentPacket;

  /**
   * Construit un container à partir du flux donné.
   *
   * @param input
   *          Le flux contenant les données audio/video à décoder.
   * @param format
   *          Le format du conteneur. Si <code>null</code> libav commencera à lire quelques ko du flux pour déduire le format.
   * @param bufferSize
   *          La taille allouée au buffer de lecture.
   */
  JAVInputContainer(final InputStream input, final AVInputFormat format, final int bufferSize) {
    this.input = input;

    // Définition du buffer, partagé avec FFMPEG natif.
    this.streamPtr = new BytePointer(avutil.av_malloc(bufferSize));
    this.streamPtr.capacity(bufferSize);

    // Préparation du contexte.
    this.ioCtx = checkAllocation(AVIOContext.class, avformat.avio_alloc_context(
        streamPtr,
        bufferSize,
        0,
        null,
        newAvIoReader(input, bufferSize),
        null,
        null));

    // Non seekable, non writable.
    this.ioCtx.seekable(0);

    // Préparation du format.
    this.formatCtx = checkAllocation(AVFormatContext.class, avformat.avformat_alloc_context());
    this.formatCtx.flags(CFlag.plus(formatCtx.flags(), AVFormatFlag.AVFMT_FLAG_CUSTOM_IO));
    this.formatCtx.pb(ioCtx);

    // Ouvre le flux et lit les entêtes. Si inputFormat est null, le format sera deviné en lisant les premier octets.
    checkAndThrow(avformat.avformat_open_input(formatCtx, (String) null, format, null));
    LOG.debug("Flux de lecture libav ouvert");

    // Récupère les informations du format.
    checkAndThrow(avformat.avformat_find_stream_info(formatCtx, (AVDictionary) null));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Format lu : Format={}, durée={}s, flux={}",
          formatCtx.iformat().long_name().getString(),
          formatCtx.duration() / 1000000l,
          formatCtx.nb_streams());
    }

    this.format = formatCtx.iformat();

    // Construit la liste de streams.
    this.streams = new ArrayList<>(formatCtx.nb_streams());
    for (int i = 0; i < formatCtx.nb_streams(); i++) {
      final JAVInputStream stream = JAVInputStream.create(this, formatCtx.streams(i));
      this.streams.add(stream);
      LOG.debug("Flux n°{} - {}", i, stream);
    }
  }

  /**
   * @return the streams
   */
  public List<JAVInputStream> getStreams() {
    return streams;
  }

  /**
   * @return the format
   */
  public AVInputFormat getFormat() {
    return format;
  }

  /**
   * @return the formatCtx
   */
  public AVFormatContext getFormatCtx() {
    return formatCtx;
  }

  /**
   * Indique au codec qu'il n'a plus besoin de conserver le paquet en mémoire.
   */
  private void unrefPacket() {
    if (currentPacket != null) {
      avcodec.av_packet_unref(currentPacket);
      currentPacket = null;
    }
  }

  /**
   * @return Un paquet lu depuis le le flux. <code>null</code> s'il n'y a plus de paquets à lire.
   */
  public JAVPacket readPacket() {
    unrefPacket();
    
    final AVPacket packet = new AVPacket();
    int ret = avformat.av_read_frame(formatCtx, packet);
    if (ret == avutil.AVERROR_EOF) {
      return null;
    }

    checkAndThrow(ret);
    currentPacket = packet;
    return new JAVPacket(streams.get(packet.stream_index()), packet);
  }

  @Override
  public void finalize() throws IOException {
    close();
  }

  @Override
  public void close() throws IOException {
    unrefPacket();

    if (streams != null) {
      // Ferme les flux.
      streams.forEach(JAVInputStream::close);
    }

    avformat.avformat_free_context(formatCtx);
    avformat.avio_context_free(ioCtx);

    avutil.av_free(streamPtr);
    streamPtr.deallocate();

    input.close();
  }
}
