/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.io;

import org.bytedeco.ffmpeg.global.avutil;

import fr.syrdek.ffmpeg.libav.java.CEnum;

/**
 * Format de pixel utilisé pour l'encodage vidéo.<br>
 * On garde le péfixe FMT_ car certaines valeurs commenceraient par un chiffre sinon.
 *
 * Généré via : <br>
 * wget --quiet -O - https://github.com/FFmpeg/FFmpeg/blob/master/libavutil/pixfmt.h | sed -e 's/>/>\n/g' -e 's/</\n</g'
 * | sed -nr 's#^\s+(AV_PIX_FMT_([^ ,=]+)).*$#FMT_\2(avutil.\1), // \2#p'
 *
 * @see https://github.com/FFmpeg/FFmpeg/blob/master/libavutil/pixfmt.h
 * @author Syrdek
 *
 */
public enum AVPixFormat implements CEnum {
  FMT_NONE(avutil.AV_PIX_FMT_NONE), // NONE
  FMT_YUV420P(avutil.AV_PIX_FMT_YUV420P), // YUV420P
  FMT_YUYV422(avutil.AV_PIX_FMT_YUYV422), // YUYV422
  FMT_RGB24(avutil.AV_PIX_FMT_RGB24), // RGB24
  FMT_BGR24(avutil.AV_PIX_FMT_BGR24), // BGR24
  FMT_YUV422P(avutil.AV_PIX_FMT_YUV422P), // YUV422P
  FMT_YUV444P(avutil.AV_PIX_FMT_YUV444P), // YUV444P
  FMT_YUV410P(avutil.AV_PIX_FMT_YUV410P), // YUV410P
  FMT_YUV411P(avutil.AV_PIX_FMT_YUV411P), // YUV411P
  FMT_GRAY8(avutil.AV_PIX_FMT_GRAY8), // GRAY8
  FMT_MONOWHITE(avutil.AV_PIX_FMT_MONOWHITE), // MONOWHITE
  FMT_MONOBLACK(avutil.AV_PIX_FMT_MONOBLACK), // MONOBLACK
  FMT_PAL8(avutil.AV_PIX_FMT_PAL8), // PAL8
  FMT_YUVJ420P(avutil.AV_PIX_FMT_YUVJ420P), // YUVJ420P
  FMT_YUVJ422P(avutil.AV_PIX_FMT_YUVJ422P), // YUVJ422P
  FMT_YUVJ444P(avutil.AV_PIX_FMT_YUVJ444P), // YUVJ444P
  FMT_UYVY422(avutil.AV_PIX_FMT_UYVY422), // UYVY422
  FMT_UYYVYY411(avutil.AV_PIX_FMT_UYYVYY411), // UYYVYY411
  FMT_BGR8(avutil.AV_PIX_FMT_BGR8), // BGR8
  FMT_BGR4(avutil.AV_PIX_FMT_BGR4), // BGR4
  FMT_BGR4_BYTE(avutil.AV_PIX_FMT_BGR4_BYTE), // BGR4_BYTE
  FMT_RGB8(avutil.AV_PIX_FMT_RGB8), // RGB8
  FMT_RGB4(avutil.AV_PIX_FMT_RGB4), // RGB4
  FMT_RGB4_BYTE(avutil.AV_PIX_FMT_RGB4_BYTE), // RGB4_BYTE
  FMT_NV12(avutil.AV_PIX_FMT_NV12), // NV12
  FMT_NV21(avutil.AV_PIX_FMT_NV21), // NV21
  FMT_ARGB(avutil.AV_PIX_FMT_ARGB), // ARGB
  FMT_RGBA(avutil.AV_PIX_FMT_RGBA), // RGBA
  FMT_ABGR(avutil.AV_PIX_FMT_ABGR), // ABGR
  FMT_BGRA(avutil.AV_PIX_FMT_BGRA), // BGRA
  FMT_GRAY16BE(avutil.AV_PIX_FMT_GRAY16BE), // GRAY16BE
  FMT_GRAY16LE(avutil.AV_PIX_FMT_GRAY16LE), // GRAY16LE
  FMT_YUV440P(avutil.AV_PIX_FMT_YUV440P), // YUV440P
  FMT_YUVJ440P(avutil.AV_PIX_FMT_YUVJ440P), // YUVJ440P
  FMT_YUVA420P(avutil.AV_PIX_FMT_YUVA420P), // YUVA420P
  FMT_RGB48BE(avutil.AV_PIX_FMT_RGB48BE), // RGB48BE
  FMT_RGB48LE(avutil.AV_PIX_FMT_RGB48LE), // RGB48LE
  FMT_RGB565BE(avutil.AV_PIX_FMT_RGB565BE), // RGB565BE
  FMT_RGB565LE(avutil.AV_PIX_FMT_RGB565LE), // RGB565LE
  FMT_RGB555BE(avutil.AV_PIX_FMT_RGB555BE), // RGB555BE
  FMT_RGB555LE(avutil.AV_PIX_FMT_RGB555LE), // RGB555LE
  FMT_BGR565BE(avutil.AV_PIX_FMT_BGR565BE), // BGR565BE
  FMT_BGR565LE(avutil.AV_PIX_FMT_BGR565LE), // BGR565LE
  FMT_BGR555BE(avutil.AV_PIX_FMT_BGR555BE), // BGR555BE
  FMT_BGR555LE(avutil.AV_PIX_FMT_BGR555LE), // BGR555LE
  FMT_VAAPI_MOCO(avutil.AV_PIX_FMT_VAAPI_MOCO), // VAAPI_MOCO
  FMT_VAAPI_IDCT(avutil.AV_PIX_FMT_VAAPI_IDCT), // VAAPI_IDCT
  FMT_VAAPI_VLD(avutil.AV_PIX_FMT_VAAPI_VLD), // VAAPI_VLD
  FMT_VAAPI(avutil.AV_PIX_FMT_VAAPI), // VAAPI
  FMT_YUV420P16LE(avutil.AV_PIX_FMT_YUV420P16LE), // YUV420P16LE
  FMT_YUV420P16BE(avutil.AV_PIX_FMT_YUV420P16BE), // YUV420P16BE
  FMT_YUV422P16LE(avutil.AV_PIX_FMT_YUV422P16LE), // YUV422P16LE
  FMT_YUV422P16BE(avutil.AV_PIX_FMT_YUV422P16BE), // YUV422P16BE
  FMT_YUV444P16LE(avutil.AV_PIX_FMT_YUV444P16LE), // YUV444P16LE
  FMT_YUV444P16BE(avutil.AV_PIX_FMT_YUV444P16BE), // YUV444P16BE
  FMT_DXVA2_VLD(avutil.AV_PIX_FMT_DXVA2_VLD), // DXVA2_VLD
  FMT_RGB444LE(avutil.AV_PIX_FMT_RGB444LE), // RGB444LE
  FMT_RGB444BE(avutil.AV_PIX_FMT_RGB444BE), // RGB444BE
  FMT_BGR444LE(avutil.AV_PIX_FMT_BGR444LE), // BGR444LE
  FMT_BGR444BE(avutil.AV_PIX_FMT_BGR444BE), // BGR444BE
  FMT_YA8(avutil.AV_PIX_FMT_YA8), // YA8
  FMT_Y400A(avutil.AV_PIX_FMT_Y400A), // Y400A
  FMT_GRAY8A(avutil.AV_PIX_FMT_GRAY8A), // GRAY8A=
  FMT_BGR48BE(avutil.AV_PIX_FMT_BGR48BE), // BGR48BE
  FMT_BGR48LE(avutil.AV_PIX_FMT_BGR48LE), // BGR48LE
  FMT_YUV420P9BE(avutil.AV_PIX_FMT_YUV420P9BE), // YUV420P9BE
  FMT_YUV420P9LE(avutil.AV_PIX_FMT_YUV420P9LE), // YUV420P9LE
  FMT_YUV420P10BE(avutil.AV_PIX_FMT_YUV420P10BE), // YUV420P10BE
  FMT_YUV420P10LE(avutil.AV_PIX_FMT_YUV420P10LE), // YUV420P10LE
  FMT_YUV422P10BE(avutil.AV_PIX_FMT_YUV422P10BE), // YUV422P10BE
  FMT_YUV422P10LE(avutil.AV_PIX_FMT_YUV422P10LE), // YUV422P10LE
  FMT_YUV444P9BE(avutil.AV_PIX_FMT_YUV444P9BE), // YUV444P9BE
  FMT_YUV444P9LE(avutil.AV_PIX_FMT_YUV444P9LE), // YUV444P9LE
  FMT_YUV444P10BE(avutil.AV_PIX_FMT_YUV444P10BE), // YUV444P10BE
  FMT_YUV444P10LE(avutil.AV_PIX_FMT_YUV444P10LE), // YUV444P10LE
  FMT_YUV422P9BE(avutil.AV_PIX_FMT_YUV422P9BE), // YUV422P9BE
  FMT_YUV422P9LE(avutil.AV_PIX_FMT_YUV422P9LE), // YUV422P9LE
  FMT_GBRP(avutil.AV_PIX_FMT_GBRP), // GBRP
  FMT_GBR24P(avutil.AV_PIX_FMT_GBR24P), // GBR24P
  FMT_GBRP9BE(avutil.AV_PIX_FMT_GBRP9BE), // GBRP9BE
  FMT_GBRP9LE(avutil.AV_PIX_FMT_GBRP9LE), // GBRP9LE
  FMT_GBRP10BE(avutil.AV_PIX_FMT_GBRP10BE), // GBRP10BE
  FMT_GBRP10LE(avutil.AV_PIX_FMT_GBRP10LE), // GBRP10LE
  FMT_GBRP16BE(avutil.AV_PIX_FMT_GBRP16BE), // GBRP16BE
  FMT_GBRP16LE(avutil.AV_PIX_FMT_GBRP16LE), // GBRP16LE
  FMT_YUVA422P(avutil.AV_PIX_FMT_YUVA422P), // YUVA422P
  FMT_YUVA444P(avutil.AV_PIX_FMT_YUVA444P), // YUVA444P
  FMT_YUVA420P9BE(avutil.AV_PIX_FMT_YUVA420P9BE), // YUVA420P9BE
  FMT_YUVA420P9LE(avutil.AV_PIX_FMT_YUVA420P9LE), // YUVA420P9LE
  FMT_YUVA422P9BE(avutil.AV_PIX_FMT_YUVA422P9BE), // YUVA422P9BE
  FMT_YUVA422P9LE(avutil.AV_PIX_FMT_YUVA422P9LE), // YUVA422P9LE
  FMT_YUVA444P9BE(avutil.AV_PIX_FMT_YUVA444P9BE), // YUVA444P9BE
  FMT_YUVA444P9LE(avutil.AV_PIX_FMT_YUVA444P9LE), // YUVA444P9LE
  FMT_YUVA420P10BE(avutil.AV_PIX_FMT_YUVA420P10BE), // YUVA420P10BE
  FMT_YUVA420P10LE(avutil.AV_PIX_FMT_YUVA420P10LE), // YUVA420P10LE
  FMT_YUVA422P10BE(avutil.AV_PIX_FMT_YUVA422P10BE), // YUVA422P10BE
  FMT_YUVA422P10LE(avutil.AV_PIX_FMT_YUVA422P10LE), // YUVA422P10LE
  FMT_YUVA444P10BE(avutil.AV_PIX_FMT_YUVA444P10BE), // YUVA444P10BE
  FMT_YUVA444P10LE(avutil.AV_PIX_FMT_YUVA444P10LE), // YUVA444P10LE
  FMT_YUVA420P16BE(avutil.AV_PIX_FMT_YUVA420P16BE), // YUVA420P16BE
  FMT_YUVA420P16LE(avutil.AV_PIX_FMT_YUVA420P16LE), // YUVA420P16LE
  FMT_YUVA422P16BE(avutil.AV_PIX_FMT_YUVA422P16BE), // YUVA422P16BE
  FMT_YUVA422P16LE(avutil.AV_PIX_FMT_YUVA422P16LE), // YUVA422P16LE
  FMT_YUVA444P16BE(avutil.AV_PIX_FMT_YUVA444P16BE), // YUVA444P16BE
  FMT_YUVA444P16LE(avutil.AV_PIX_FMT_YUVA444P16LE), // YUVA444P16LE
  FMT_VDPAU(avutil.AV_PIX_FMT_VDPAU), // VDPAU
  FMT_XYZ12LE(avutil.AV_PIX_FMT_XYZ12LE), // XYZ12LE
  FMT_XYZ12BE(avutil.AV_PIX_FMT_XYZ12BE), // XYZ12BE
  FMT_NV16(avutil.AV_PIX_FMT_NV16), // NV16
  FMT_NV20LE(avutil.AV_PIX_FMT_NV20LE), // NV20LE
  FMT_NV20BE(avutil.AV_PIX_FMT_NV20BE), // NV20BE
  FMT_RGBA64BE(avutil.AV_PIX_FMT_RGBA64BE), // RGBA64BE
  FMT_RGBA64LE(avutil.AV_PIX_FMT_RGBA64LE), // RGBA64LE
  FMT_BGRA64BE(avutil.AV_PIX_FMT_BGRA64BE), // BGRA64BE
  FMT_BGRA64LE(avutil.AV_PIX_FMT_BGRA64LE), // BGRA64LE
  FMT_YVYU422(avutil.AV_PIX_FMT_YVYU422), // YVYU422
  FMT_YA16BE(avutil.AV_PIX_FMT_YA16BE), // YA16BE
  FMT_YA16LE(avutil.AV_PIX_FMT_YA16LE), // YA16LE
  FMT_GBRAP(avutil.AV_PIX_FMT_GBRAP), // GBRAP
  FMT_GBRAP16BE(avutil.AV_PIX_FMT_GBRAP16BE), // GBRAP16BE
  FMT_GBRAP16LE(avutil.AV_PIX_FMT_GBRAP16LE), // GBRAP16LE
  FMT_QSV(avutil.AV_PIX_FMT_QSV), // QSV
  FMT_MMAL(avutil.AV_PIX_FMT_MMAL), // MMAL
  FMT_D3D11VA_VLD(avutil.AV_PIX_FMT_D3D11VA_VLD), // D3D11VA_VLD
  FMT_CUDA(avutil.AV_PIX_FMT_CUDA), // CUDA
  FMT_0RGB(avutil.AV_PIX_FMT_0RGB), // 0RGB
  FMT_RGB0(avutil.AV_PIX_FMT_RGB0), // RGB0
  FMT_0BGR(avutil.AV_PIX_FMT_0BGR), // 0BGR
  FMT_BGR0(avutil.AV_PIX_FMT_BGR0), // BGR0
  FMT_YUV420P12BE(avutil.AV_PIX_FMT_YUV420P12BE), // YUV420P12BE
  FMT_YUV420P12LE(avutil.AV_PIX_FMT_YUV420P12LE), // YUV420P12LE
  FMT_YUV420P14BE(avutil.AV_PIX_FMT_YUV420P14BE), // YUV420P14BE
  FMT_YUV420P14LE(avutil.AV_PIX_FMT_YUV420P14LE), // YUV420P14LE
  FMT_YUV422P12BE(avutil.AV_PIX_FMT_YUV422P12BE), // YUV422P12BE
  FMT_YUV422P12LE(avutil.AV_PIX_FMT_YUV422P12LE), // YUV422P12LE
  FMT_YUV422P14BE(avutil.AV_PIX_FMT_YUV422P14BE), // YUV422P14BE
  FMT_YUV422P14LE(avutil.AV_PIX_FMT_YUV422P14LE), // YUV422P14LE
  FMT_YUV444P12BE(avutil.AV_PIX_FMT_YUV444P12BE), // YUV444P12BE
  FMT_YUV444P12LE(avutil.AV_PIX_FMT_YUV444P12LE), // YUV444P12LE
  FMT_YUV444P14BE(avutil.AV_PIX_FMT_YUV444P14BE), // YUV444P14BE
  FMT_YUV444P14LE(avutil.AV_PIX_FMT_YUV444P14LE), // YUV444P14LE
  FMT_GBRP12BE(avutil.AV_PIX_FMT_GBRP12BE), // GBRP12BE
  FMT_GBRP12LE(avutil.AV_PIX_FMT_GBRP12LE), // GBRP12LE
  FMT_GBRP14BE(avutil.AV_PIX_FMT_GBRP14BE), // GBRP14BE
  FMT_GBRP14LE(avutil.AV_PIX_FMT_GBRP14LE), // GBRP14LE
  FMT_YUVJ411P(avutil.AV_PIX_FMT_YUVJ411P), // YUVJ411P
  FMT_BAYER_BGGR8(avutil.AV_PIX_FMT_BAYER_BGGR8), // BAYER_BGGR8
  FMT_BAYER_RGGB8(avutil.AV_PIX_FMT_BAYER_RGGB8), // BAYER_RGGB8
  FMT_BAYER_GBRG8(avutil.AV_PIX_FMT_BAYER_GBRG8), // BAYER_GBRG8
  FMT_BAYER_GRBG8(avutil.AV_PIX_FMT_BAYER_GRBG8), // BAYER_GRBG8
  FMT_BAYER_BGGR16LE(avutil.AV_PIX_FMT_BAYER_BGGR16LE), // BAYER_BGGR16LE
  FMT_BAYER_BGGR16BE(avutil.AV_PIX_FMT_BAYER_BGGR16BE), // BAYER_BGGR16BE
  FMT_BAYER_RGGB16LE(avutil.AV_PIX_FMT_BAYER_RGGB16LE), // BAYER_RGGB16LE
  FMT_BAYER_RGGB16BE(avutil.AV_PIX_FMT_BAYER_RGGB16BE), // BAYER_RGGB16BE
  FMT_BAYER_GBRG16LE(avutil.AV_PIX_FMT_BAYER_GBRG16LE), // BAYER_GBRG16LE
  FMT_BAYER_GBRG16BE(avutil.AV_PIX_FMT_BAYER_GBRG16BE), // BAYER_GBRG16BE
  FMT_BAYER_GRBG16LE(avutil.AV_PIX_FMT_BAYER_GRBG16LE), // BAYER_GRBG16LE
  FMT_BAYER_GRBG16BE(avutil.AV_PIX_FMT_BAYER_GRBG16BE), // BAYER_GRBG16BE
  FMT_XVMC(avutil.AV_PIX_FMT_XVMC), // XVMC
  FMT_YUV440P10LE(avutil.AV_PIX_FMT_YUV440P10LE), // YUV440P10LE
  FMT_YUV440P10BE(avutil.AV_PIX_FMT_YUV440P10BE), // YUV440P10BE
  FMT_YUV440P12LE(avutil.AV_PIX_FMT_YUV440P12LE), // YUV440P12LE
  FMT_YUV440P12BE(avutil.AV_PIX_FMT_YUV440P12BE), // YUV440P12BE
  FMT_AYUV64LE(avutil.AV_PIX_FMT_AYUV64LE), // AYUV64LE
  FMT_AYUV64BE(avutil.AV_PIX_FMT_AYUV64BE), // AYUV64BE
  FMT_VIDEOTOOLBOX(avutil.AV_PIX_FMT_VIDEOTOOLBOX), // VIDEOTOOLBOX
  FMT_P010LE(avutil.AV_PIX_FMT_P010LE), // P010LE
  FMT_P010BE(avutil.AV_PIX_FMT_P010BE), // P010BE
  FMT_GBRAP12BE(avutil.AV_PIX_FMT_GBRAP12BE), // GBRAP12BE
  FMT_GBRAP12LE(avutil.AV_PIX_FMT_GBRAP12LE), // GBRAP12LE
  FMT_GBRAP10BE(avutil.AV_PIX_FMT_GBRAP10BE), // GBRAP10BE
  FMT_GBRAP10LE(avutil.AV_PIX_FMT_GBRAP10LE), // GBRAP10LE
  FMT_MEDIACODEC(avutil.AV_PIX_FMT_MEDIACODEC), // MEDIACODEC
  FMT_GRAY12BE(avutil.AV_PIX_FMT_GRAY12BE), // GRAY12BE
  FMT_GRAY12LE(avutil.AV_PIX_FMT_GRAY12LE), // GRAY12LE
  FMT_GRAY10BE(avutil.AV_PIX_FMT_GRAY10BE), // GRAY10BE
  FMT_GRAY10LE(avutil.AV_PIX_FMT_GRAY10LE), // GRAY10LE
  FMT_P016LE(avutil.AV_PIX_FMT_P016LE), // P016LE
  FMT_P016BE(avutil.AV_PIX_FMT_P016BE), // P016BE
  FMT_D3D11(avutil.AV_PIX_FMT_D3D11), // D3D11
  FMT_GRAY9BE(avutil.AV_PIX_FMT_GRAY9BE), // GRAY9BE
  FMT_GRAY9LE(avutil.AV_PIX_FMT_GRAY9LE), // GRAY9LE
  FMT_GBRPF32BE(avutil.AV_PIX_FMT_GBRPF32BE), // GBRPF32BE
  FMT_GBRPF32LE(avutil.AV_PIX_FMT_GBRPF32LE), // GBRPF32LE
  FMT_GBRAPF32BE(avutil.AV_PIX_FMT_GBRAPF32BE), // GBRAPF32BE
  FMT_GBRAPF32LE(avutil.AV_PIX_FMT_GBRAPF32LE), // GBRAPF32LE
  FMT_DRM_PRIME(avutil.AV_PIX_FMT_DRM_PRIME), // DRM_PRIME
  FMT_OPENCL(avutil.AV_PIX_FMT_OPENCL), // OPENCL
  FMT_GRAY14BE(avutil.AV_PIX_FMT_GRAY14BE), // GRAY14BE
  FMT_GRAY14LE(avutil.AV_PIX_FMT_GRAY14LE), // GRAY14LE
  FMT_GRAYF32BE(avutil.AV_PIX_FMT_GRAYF32BE), // GRAYF32BE
  FMT_GRAYF32LE(avutil.AV_PIX_FMT_GRAYF32LE), // GRAYF32LE
  FMT_NB(avutil.AV_PIX_FMT_NB); // NB

  private final int value;

  /**
   * @param value
   *          La valeur de l'enum C correspondante.
   */
  private AVPixFormat(int value) {
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
  public static AVPixFormat get(int value) {
    for (AVPixFormat v : values()) {
      if (v.value == value) {
        return v;
      }
    }
    return AVPixFormat.FMT_NONE;
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