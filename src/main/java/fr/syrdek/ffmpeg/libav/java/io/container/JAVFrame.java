/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io.container;

import org.bytedeco.javacpp.avutil.AVFrame;

import fr.syrdek.ffmpeg.libav.java.io.stream.in.JAVInputStream;

/**
 * Contient les données d'un paquet d'informations (audio, vidéo, sous-titre ou autre) décodées.<br>
 * Une frame est généralement le résultat du décodage d'un {@link JAVPacket}.
 * 
 * @author Syrdek
 */
public class JAVFrame {
  private final JAVInputStream origin;
  private final AVFrame frame;

  /**
   * Construit un paquet de données.<br>
   * 
   * @param origin
   *          Le flux d'origine du paquet.
   * @param frame
   *          Les données décodées du paquet.
   */
  public JAVFrame(final JAVInputStream origin, final AVFrame frame) {
    this.origin = origin;
    this.frame = frame;
  }

  /**
   * @return the origin
   */
  public JAVInputStream getOrigin() {
    return origin;
  }

  /**
   * @return the frame
   */
  public AVFrame getFrame() {
    return frame;
  }
}
