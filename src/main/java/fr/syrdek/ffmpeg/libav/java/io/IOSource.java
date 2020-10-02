package fr.syrdek.ffmpeg.libav.java.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.tests.jav.impl.container.JAVOutputContainer;

/**
 * Classe statique permettant de créer des sources de données ffmpeg à partir de streams java.
 *
 * @author t0087865
 *
 */
public class IOSource {
  private static final Logger LOG = LoggerFactory.getLogger(JAVOutputContainer.class);

  /**
   * Classe statique, pas de constructeur.
   */
  private IOSource() {
    super();
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
}
