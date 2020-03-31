package fr.syrdek.ffmpeg.libav.java.io;

import fr.syrdek.ffmpeg.libav.java.CEnum;

/**
 * Niveau de compliance de la spec a respecter lors de l'encodage.
 * 
 * @see https://ffmpeg.org/doxygen/2.4/libavcodec_2avcodec_8h.html#a96808e3862c53c7edb4ace1b2f3e544f
 * @author Syrdek
 *
 */
public enum AVEncodingCompliance implements CEnum {
  /**
   * Strictly conform to an older more strict version of the spec or reference software.
   */
  VERY_STRICT(2),
  /**
   * Strictly conform to all the things in the spec no matter what consequences.
   */
  STRICT(1),
  /**
   * 
   */
  NORMAL(0),
  /**
   * Allow unofficial extensions.
   */
  UNOFFICIAL(-1),
  /**
   * Allow nonstandardized experimental things.
   */
  EXPERIMENTAL(-2);

  /**
   * Retrouve le {@link AVEncodingCompliance} ayant la valeur donnée.
   * 
   * @param value
   *          La valeur recherchée.
   * @return L'enum correspondant. <code>null</code> si la valeur donnée ne correspond à aucune valeur connue.
   */
  public static AVEncodingCompliance get(int value) {
    for (AVEncodingCompliance v : values()) {
      if (v.value == value) {
        return v;
      }
    }
    return null;
  }

  private final int value;

  private AVEncodingCompliance(int value) {
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
