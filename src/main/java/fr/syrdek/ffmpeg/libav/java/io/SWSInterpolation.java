/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.io;

import org.bytedeco.ffmpeg.global.swscale;

import fr.syrdek.ffmpeg.libav.java.CEnum;

/**
 * Interpolation permettant de redimensionner une image.
 *
 * @see https://ffmpeg.org/ffmpeg-scaler.html
 * @author Syrdek
 *
 */
public enum SWSInterpolation implements CEnum {
  DEFAULT(swscale.SWS_BICUBIC), // Valeur par défaut.
  FAST_BILINEAR(swscale.SWS_FAST_BILINEAR), // fast bilinear scaling algorithm.
  BILINEAR(swscale.SWS_BILINEAR), // bilinear scaling algorithm.
  BICUBIC(swscale.SWS_BICUBIC), // bicubic scaling algorithm.
  X(swscale.SWS_X), // experimental scaling algorithm.
  POINT(swscale.SWS_POINT), // nearest neighbor rescaling algorithm.
  AREA(swscale.SWS_AREA), // averaging area rescaling algorithm.
  BICUBLIN(swscale.SWS_BICUBLIN), // bicubic scaling algorithm for the luma component, bilinear for chroma components.
  GAUSS(swscale.SWS_GAUSS), // Gaussian rescaling algorithm.
  SINC(swscale.SWS_SINC), // sinc rescaling algorithm.
  LANCZOS(swscale.SWS_LANCZOS), // Lanczos rescaling algorithm. The default width (alpha) is 3 and can be changed by
                                // setting param0.
  SPLINE(swscale.SWS_SPLINE); // natural bicubic spline rescaling algorithm.

  private final int value;

  /**
   * @param value
   *          La valeur de l'enum C correspondante.
   */
  private SWSInterpolation(int value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  @Override
  public int value() {
    return value;
  }

  /**
   * Retrouve le {@link SWSInterpolation} ayant la valeur donnée.
   *
   * @param value
   *          La valeur recherchée.
   * @return L'enum correspondant. <code>null</code> si la valeur donnée ne correspond à aucun format connu.
   */
  public static SWSInterpolation get(int value) {
    for (SWSInterpolation v : values()) {
      if (v.value == value) {
        return v;
      }
    }
    // Valeur par défaut.
    return SWSInterpolation.DEFAULT;
  }

  /**
   * @return La représentation textuelle de l'enum.
   */
  @Override
  public String toString() {
    return new StringBuilder(name()).append("(n°").append(value).append(")")
        .toString();
  }
}
