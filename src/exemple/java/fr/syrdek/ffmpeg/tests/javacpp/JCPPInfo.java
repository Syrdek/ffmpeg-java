package fr.syrdek.ffmpeg.tests.javacpp;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.CFlag;
import fr.syrdek.ffmpeg.libav.java.FFmpegException;
import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.AVFormatFlag;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;
import fr.syrdek.ffmpeg.tests.Utils;

/**
 * 
 * @author Syrdek
 */
public class JCPPInfo {
  private static final Logger LOG = LoggerFactory.getLogger(JCPPInfo.class);
  private static final int BUFFER_SIZE = 64 * 1024;
  
  static {
    // S'assure que les libs natives soient bien chargées.
    FFmpegNatives.ensureLoaded();
  }
  
  public static void main(String[] args) throws Exception {
    Utils.cleanup();
    
    try (final InputStream in = new FileInputStream("samples/audio.mp2")) {
      new JCPPInfo().printInfos(in);
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }

  /**
   *
   * @param in
   * @throws IOException
   */
  public void printInfos(final InputStream in) throws IOException {
    final BytePointer streamPtr = new BytePointer(avutil.av_malloc(BUFFER_SIZE));
    streamPtr.capacity(BUFFER_SIZE);

    final AVIOContext ioCtx = checkAllocation(AVIOContext.class, avformat.avio_alloc_context(
        streamPtr,
        BUFFER_SIZE,
        0,
        null,
        JAVInputContainer.newAvIoReader(in, BUFFER_SIZE),
        null,
        null));

    // Non seekable, non writable.
    ioCtx.seekable(0);
    ioCtx.write_flag(0);

    final AVFormatContext formatCtx = checkAllocation(AVFormatContext.class, avformat.avformat_alloc_context());
    formatCtx.flags(CFlag.plus(formatCtx.flags(), AVFormatFlag.CUSTOM_IO));
    formatCtx.pb(ioCtx);

    // Ouvre le flux et lit les entêtes.
    checkAndThrow(avformat.avformat_open_input(formatCtx, (String) null, null, null));

    // Récupère les informations du format.
    checkAndThrow(avformat.avformat_find_stream_info(formatCtx, (AVDictionary) null));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Flux FFMPEG ouvert. Format={}, durée={}s, flux={}",
          formatCtx.iformat().long_name().getString(),
          formatCtx.duration() / 1000000l,
          formatCtx.nb_streams());
    }

    // Parcours les flux du format.
    for (int i = 0; i < formatCtx.nb_streams(); i++) {
      // Récupère les informations de codage.
      final AVCodecParameters codecParams = formatCtx.streams(i).codecpar();
      final AVCodec codec = avcodec.avcodec_find_decoder(codecParams.codec_id());
      if (LOG.isDebugEnabled()) {
        // Un peu de debug pour s'y retrouver.
        switch (codecParams.codec_type()) {
        case avutil.AVMEDIA_TYPE_AUDIO:
          LOG.debug("Stream {} : type=Audio, codec={}, id={}, channels={}, sample rate={}", i, codec.long_name().getString(), codec.id(), codecParams.channels(), codecParams.sample_rate());
          break;
        case avutil.AVMEDIA_TYPE_VIDEO:
          LOG.debug("Stream {} : type=Video, codec={}, id={}, resolution {}x{}", i, codec.long_name().getString(), codec.id(), codecParams.width(), codecParams.height());
          break;
        default:
          LOG.debug("Stream {} : type={}, codec={}, id={}", i, codecParams.codec_type(), codec.long_name().getString(), codec.id());
          break;
        }
      }

      // Ouvre le codec.
      final AVCodecContext codecCtx = checkAllocation(AVCodecContext.class, avcodec.avcodec_alloc_context3(codec));
      checkAndThrow(avcodec.avcodec_parameters_to_context(codecCtx, codecParams));
      checkAndThrow(avcodec.avcodec_open2(codecCtx, codec, (AVDictionary) null));

      final AVPacket packet =  checkAllocation(AVPacket.class, avcodec.av_packet_alloc());
      final AVFrame frame =  checkAllocation(AVFrame.class, avutil.av_frame_alloc());

      // Lit un paquet de données brutes depuis le flux.
      while (avformat.av_read_frame(formatCtx, packet) >= 0) {
        // Décode le paquet lu.
        decodePacket(codecCtx, packet, frame);
      }

      avutil.av_frame_free(frame);
      avcodec.av_packet_free(packet);
      avcodec.avcodec_free_context(codecCtx);
    }

    avformat.avformat_free_context(formatCtx);
    avformat.avio_context_free(ioCtx);

    avutil.av_free(streamPtr);
    streamPtr.deallocate();
  }

  /**
   * Décode un paquet de données.
   * 
   * @param codecCtx
   *          Le contexte du codec à utiliser.
   * @param packet
   *          Le paquet à décoder.
   * @param frame
   *          La frame à remplir avec les données décodées.
   */
  private void decodePacket(final AVCodecContext codecCtx, final AVPacket packet, final AVFrame frame) {
    // Envoie le paquet récupéré au codec pour décodage.
    int ret = FFmpegException.checkAndThrow(avcodec.avcodec_send_packet(codecCtx, packet));
    while (ret >= 0) {
      // Récupère la frame décodée par le codec.
      ret = avcodec.avcodec_receive_frame(codecCtx, frame);
      if (ret == avutil.AVERROR_EOF || ret == avutil.AVERROR_EAGAIN()) {
        // Ces erreurs indiquent la fin de traitement et sont attendues.
        break;
      }
      // Les autres codes d'erreur sont graves.
      FFmpegException.checkAndThrow(ret);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Frame : Size={}, number={}, PTS={}, DTS={}, key_frame={}", frame.pkt_size(), codecCtx.frame_number(), frame.pts(), frame.pkt_dts(), frame.key_frame());
      }
    }
  }
}
