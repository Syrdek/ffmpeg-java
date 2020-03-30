package fr.syrdek.ffmpeg.libav.java.io;

import fr.syrdek.ffmpeg.libav.java.CFlag;

/**
 * Drapeaux de s'appliquant à un format audio/video.
 * @see https://www.ffmpeg.org/doxygen/4.1/avio_8h.html
 * 
 * @author Syrdek
 */
public enum AVSeekFlag implements CFlag {
  /**
   * Passing this as the "whence" parameter to a seek function causes it to return the filesize without seeking anywhere.
   * If it is not supported then the seek function will return <0.
   */
  AVSEEK_SIZE(0x10000),
  /**
  * Oring this flag as into the "whence" parameter to a seek function causes it to
  * seek by any means (like reopening and linear reading) or other normally unreasonable
  * means that can be extremely slow.
  * This may be ignored by the seek code.
  */
  AVSEEK_FORCE(0x20000);

  private final int value;

  AVSeekFlag(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
}