package fr.syrdek.ffmpeg.tests.jav.impl.container;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
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
import fr.syrdek.ffmpeg.libav.java.io.IOSource;
import fr.syrdek.ffmpeg.tests.jav.impl.stream.in.JAVInputStream;

/**
 * Permet de lire dans un conteneur Audio/Video.<br>
 * Cet objet doit être construit en passant par un {@link JAVInputContainer.Builder}, qui permet de gérer les paramètres
 * obligatoires et optionels.
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
      format = avformat.av_find_input_format(name);
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
     * @throws IOException
     *           Si le flux en lecture ne peut pas être lu.
     */
    public JAVInputContainer build(final InputStream in) throws IOException {
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
          int read = 0;
          buffer.position(0);
          while (read < len) {
            // Petite optimisation :
            // Si on a déjà lu quelques données, et qu'il n'y en a plus de disponible dans
            // le stream,
            // on retourne tout de suite les données qu'on a.
            // Ceci permet de laisser libav traiter le peu de données qu'on a récupéré,
            // pendant que le stream se remplit.
            if (read > 0 && in.available() <= 0) {
              return read;
            }

            int nb = in.read(dataBuffer, 0, Math.min(len - read, dataBuffer.length));
            if (nb <= 0) {
              LOG.debug("Fin de lecture");
              // Informe ffmpeg que le flux est terminé.
              return avutil.AVERROR_EOF;
            }
            buffer.put(dataBuffer);
            read += nb;
          }
          return read;
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

  // Passe à true quand le conteneur est fermé.
  private boolean closed = false;

  /**
   * Construit un container à partir du flux donné.
   *
   * @param input
   *          Le flux contenant les données audio/video à décoder.
   * @param format
   *          Le format du conteneur. Si <code>null</code> libav commencera à lire quelques ko du flux pour déduire le
   *          format.
   * @param bufferSize
   *          La taille allouée au buffer de lecture.
   * @throws IOException
   *           Si une erreur de lecture intervient.
   */
  protected JAVInputContainer(final InputStream input, final AVInputFormat format, final int bufferSize)
      throws IOException {
    try {
      this.input = input;

      // Définition du buffer, partagé avec FFMPEG natif.
      streamPtr = new BytePointer(avutil.av_malloc(bufferSize));
      streamPtr.capacity(bufferSize);

      // Préparation du contexte.
      ioCtx = checkAllocation(AVIOContext.class,
          avformat.avio_alloc_context(streamPtr, bufferSize, 0, null, IOSource.newAvIoReader(input, bufferSize), null,
              null));

      // Non seekable, non writable.
      ioCtx.seekable(0);

      // Préparation du format.
      formatCtx = checkAllocation(AVFormatContext.class, avformat.avformat_alloc_context());
      formatCtx.flags(CFlag.plus(formatCtx.flags(), AVFormatFlag.CUSTOM_IO));
      formatCtx.pb(ioCtx);

      // Ouvre le flux et lit les entêtes. Si inputFormat est null, le format sera
      // deviné en lisant les premier octets.
      checkAndThrow(avformat.avformat_open_input(formatCtx, (String) null, format, null));
      LOG.debug("Flux de lecture libav ouvert");

      // Récupère les informations du format.
      checkAndThrow(avformat.avformat_find_stream_info(formatCtx, (AVDictionary) null));
      if (LOG.isDebugEnabled()) {
        LOG.debug("Format lu : Format={}, durée={}s, flux={}", formatCtx.iformat().long_name().getString(),
            formatCtx.duration() / 1000000l, formatCtx.nb_streams());
      }

      this.format = formatCtx.iformat();

      // Construit la liste de streams.
      streams = new ArrayList<>(formatCtx.nb_streams());
      for (int i = 0; i < formatCtx.nb_streams(); i++) {
        final JAVInputStream stream = JAVInputStream.create(this, formatCtx.streams(i));
        streams.add(stream);
        LOG.debug("Flux n°{} - {}", i, stream);
      }
    } catch (Exception e) {
      // Nettoie la mémoire allouée.
      close();
      throw e;
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
   * Lit entièrement le fichier, et envoie les paquets codés au consumer donné.
   *
   * @param packetConsumer
   *          Le consumer a notifier chaque fois qu'un paquet est lu.
   */
  public void readFully(final Consumer<JAVPacket> packetConsumer) {
    final AVPacket packet = avcodec.av_packet_alloc();
    try {
      int ret = 0;
      do {
        ret = avformat.av_read_frame(formatCtx, packet);

        // Vérifie qu'on a pas atteint la fin du fichier.
        if (ret != avutil.AVERROR_EOF) {
          // Vérifie le code d'erreur.
          checkAndThrow(ret);
          // Transmet le paquet au consumer.
          packetConsumer.accept(new JAVPacket(streams.get(packet.stream_index()), packet));
          // Libère le paquet.
          avcodec.av_packet_unref(packet);
        }
      } while (ret >= 0);
    } finally {
      // Nettoie la mémoire allouée.
      avcodec.av_packet_free(packet);
    }
  }

  /**
   * Lit entièrement le fichier, et décode les paquets. Les paquets décodés sont donnés au consumer donné.
   *
   * @param frameConsumer
   *          Le consumer a notifier chaque fois qu'un paquet est décodé.
   */
  public void decodeFully(final Consumer<JAVFrame> frameConsumer) {
    readFully(t -> t.getOrigin().decode(t, frameConsumer));
  }

  @Override
  public void finalize() throws IOException {
    close();
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      // Déjà fermé, rien à faire de nouveau.
      return;
    }

    if (streams != null) {
      // Ferme les flux.
      streams.forEach(JAVInputStream::close);
    }

    // Peut être nul en cas d'erreur durant le constructeur uniquement.
    if (formatCtx != null) {
      avformat.avformat_free_context(formatCtx);
    }

    // Peut être nul en cas d'erreur durant le constructeur uniquement.
    if (ioCtx != null) {
      avformat.avio_context_free(ioCtx);
    }

    // Peut être nul en cas d'erreur durant le constructeur uniquement.
    if (streamPtr != null) {
      avutil.av_free(streamPtr);
      streamPtr.deallocate();
    }

    input.close();
    closed = true;
  }
}
