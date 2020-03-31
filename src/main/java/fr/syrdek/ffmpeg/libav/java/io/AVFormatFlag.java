package fr.syrdek.ffmpeg.libav.java.io;

import fr.syrdek.ffmpeg.libav.java.CFlag;

/**
 * Drapeaux de s'appliquant à un format audio/video.
 * 
 * @see https://www.ffmpeg.org/doxygen/4.1/avformat_8h.html
 * 
 * @author Syrdek
 */
public enum AVFormatFlag implements CFlag {
  /**
   * signal that no header is present (streams are added dynamically).
   */
  GENPTS(0x0001),
  /**
   * Ignore index.
   */
  IGNIDX(0x0002),
  /**
   * Do not block when reading packets from input.
   */
  NONBLOCK(0x0004),
  /**
   * Ignore DTS on frames that contain both DTS & PTS.
   */
  IGNDTS(0x0008),
  /**
   * Do not infer any values from other values, just return what is stored in the container.
   */
  NOFILLIN(0x0010),
  /**
   * Do not use AVParsers, you also must set AVFMT_FLAG_NOFILLIN as the fillin code works on frames and no parsing -> no frames.
   * Also seeking to frames can not work if parsing to find frame boundaries has been disabled.
   */
  NOPARSE(0x0020),
  /**
   * Do not buffer frames when possible.
   */
  NOBUFFER(0x0040),
  /**
   * The caller has supplied a custom AVIOContext, don't avio_close() it.
   */
  CUSTOM_IO(0x0080),
  /**
   * Discard frames marked corrupted.
   */
  DISCARD_CORRUPT(0x0100),
  /**
   * Flush the AVIOContext every packet.
   */
  FLUSH_PACKETS(0x0200),
  /**
   * When muxing, try to avoid writing any random/volatile data to the output.
   */
  BITEXACT(0x0400),
  /**
   * try to interleave outputted packets by dts (using this flag can slow demuxing down)
   */
  SORT_DTS(0x1000),
  /**
   * Enable use of private options by delaying codec open (this could be made default once all code is converted)
   */
  PRIV_OPT(0x2000);

  private final int value;

  private AVFormatFlag(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
  
  /**
   * @return La représentation textuelle de l'enum.
   */
  public String toString() {
    return new StringBuilder(name()).append("(").append(value).append(")").toString();
  }
}