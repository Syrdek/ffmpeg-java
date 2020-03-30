/**
 * 
 */
package fr.syrdek.ffmpeg.libav.java.io.container;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVStream;

import fr.syrdek.ffmpeg.libav.java.io.stream.in.JAVInputStream;

/**
 * Contient les données sur un paquet d'informations à décoder.
 * 
 * @author Syrdek
 */
public class JAVPacket {
  private final JAVInputStream origin;
  private final AVPacket packet;

  /**
   * Construit un paquet de données.
   * 
   * @param origin
   *          Le flux d'origine du paquet.
   * @param packet
   *          Les données du paquet.
   */
  public JAVPacket(JAVInputStream origin, AVPacket packet) {
    this.origin = origin;
    this.packet = packet;
  }

  /**
   * @return the origin
   */
  public JAVInputStream getOrigin() {
    return origin;
  }

  /**
   * @return the packet
   */
  public AVPacket getPacket() {
    return packet;
  }
}
