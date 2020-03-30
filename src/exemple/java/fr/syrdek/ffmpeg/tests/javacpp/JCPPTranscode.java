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
import org.bytedeco.javacpp.avformat.AVOutputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.CFlag;
import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.AVFormatFlag;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVOutputContainer;
import fr.syrdek.ffmpeg.tests.Utils;

/**
 * 
 * @author Syrdek
 */
public class JCPPTranscode {
  private static final Logger LOG = LoggerFactory.getLogger(JCPPTranscode.class);
  private static final int BUFFER_SIZE = 256 * 1024;

  public static void main(String[] args) throws Exception {
    Utils.cleanup();
    
    try (final InputStream in = new FileInputStream("samples/audio.mp2");
        final OutputStream out = new FileOutputStream("target/result.mkv")) {
      new JCPPTranscode().withFormatName("matroska").transcode(in, out);
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }
  
  private AVFormatContext inFormatCtx;
  private BytePointer inStreamPtr;
  private AVIOContext ioInCtx;

  private AVFormatContext outFormatCtx;
  private AVOutputFormat outFormat;
  private BytePointer outStreamPtr;
  private AVIOContext ioOutCtx;

  static {
    // S'assure que les libs natives soient bien chargées.
    FFmpegNatives.ensureLoaded();
  }

  private void openInput(final InputStream in) {
    inStreamPtr = new BytePointer(avutil.av_malloc(BUFFER_SIZE));
    inStreamPtr.capacity(BUFFER_SIZE);

    ioInCtx = checkAllocation(AVIOContext.class, avformat.avio_alloc_context(
        inStreamPtr,
        BUFFER_SIZE,
        0,
        null,
        JAVInputContainer.newAvIoReader(in, BUFFER_SIZE),
        null,
        null));

    // Non seekable, non writable.
    ioInCtx.seekable(0);

    inFormatCtx = checkAllocation(AVFormatContext.class, avformat.avformat_alloc_context());
    inFormatCtx.flags(CFlag.plus(inFormatCtx.flags(), AVFormatFlag.AVFMT_FLAG_CUSTOM_IO));
    inFormatCtx.pb(ioInCtx);

    // Ouvre le flux et lit les entêtes.
    checkAndThrow(avformat.avformat_open_input(inFormatCtx, (String) null, null, null));

    // Récupère les informations du format.
    checkAndThrow(avformat.avformat_find_stream_info(inFormatCtx, (AVDictionary) null));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Flux FFMPEG ouvert. Format={}, durée={}s, flux={}",
          inFormatCtx.iformat().long_name().getString(),
          inFormatCtx.duration() / 1000000l,
          inFormatCtx.nb_streams());
    }
  }

  private void openOutput(final OutputStream out) {
    outStreamPtr = new BytePointer(avutil.av_malloc(BUFFER_SIZE));
    outStreamPtr.capacity(BUFFER_SIZE);

    ioOutCtx = checkAllocation(AVIOContext.class, avformat.avio_alloc_context(
        outStreamPtr,
        64 * 1024,
        1,
        null,
        null,
        JAVOutputContainer.newAvIoWriter(out, BUFFER_SIZE),
        null));

    // Non seekable, non writable.
    ioOutCtx.seekable(0);
    ioOutCtx.max_packet_size(BUFFER_SIZE);

    outFormatCtx = checkAllocation(AVFormatContext.class, avformat.avformat_alloc_context());
    outFormatCtx.flags(CFlag.plus(outFormatCtx.flags(), AVFormatFlag.AVFMT_FLAG_CUSTOM_IO));

    checkAndThrow(avformat.avformat_alloc_output_context2(outFormatCtx, outFormat, (String) null, null));
    outFormatCtx.pb(ioOutCtx);
  }

  /**
   *
   * @param in
   * @throws IOException
   */
  public void transcode(final InputStream in, final OutputStream out) throws IOException {
    openInput(in);
    openOutput(out);

    final int nbInStreams = inFormatCtx.nb_streams();

    final AVPacket packet = new AVPacket();
    final AVCodecContext[] codecsCtx = new AVCodecContext[nbInStreams];
    
    for (int i = 0; i < nbInStreams; i++) {
      final AVStream inStream = inFormatCtx.streams(i);
      final AVCodecParameters codecPar = inStream.codecpar();
      final AVCodec codec = checkAllocation(avcodec.avcodec_find_decoder(codecPar.codec_id()));
      final AVCodecContext codecCtx = checkAllocation(avcodec.avcodec_alloc_context3(codec));
      checkAndThrow(avcodec.avcodec_parameters_to_context(codecCtx, codecPar));
      checkAndThrow(avcodec.avcodec_open2(codecCtx, codec, (AVDictionary)null));
      
      
      final AVStream outStream = checkAllocation(avformat.avformat_new_stream(outFormatCtx, null));
      
      // Aucun transcodage.
      checkAndThrow(avcodec.avcodec_parameters_copy(outStream.codecpar(), codecPar));
      outStream.codecpar().codec_tag(0);
      
      codecsCtx[i] = codecCtx;
    }
    
    checkAndThrow(avformat.avformat_write_header(outFormatCtx, (AVDictionary)null));
    
    for (;;) {
      int ret = avformat.av_read_frame(inFormatCtx, packet);
      if (ret == avutil.AVERROR_EOF) {
        LOG.debug("Fin de stream atteinte (code={}).", ret, avutil.AVERROR_EOF, avutil.AVERROR_EAGAIN());
        break;
      }
      // Gestion des autres cas d'erreur.
      checkAndThrow(ret);

      final AVStream inStream = inFormatCtx.streams(packet.stream_index());
      final AVStream outStream = outFormatCtx.streams(packet.stream_index());

      packet.stream_index(outStream.index());
      // Recalcule les PTS (presentation timestamp), DTS (decoding timestamp), et la durée de l'image en fonction de la nouvelle base de temps du conteneur.
      // Voir http://dranger.com/ffmpeg/tutorial05.html pour plus d'explications.
      avcodec.av_packet_rescale_ts(packet, inStream.time_base(), outStream.time_base());

      checkAndThrow(avformat.av_interleaved_write_frame(outFormatCtx, packet));
      avcodec.av_packet_unref(packet);
    }

    checkAndThrow(avformat.av_write_trailer(outFormatCtx));
    
    
    for (int i = 0; i < codecsCtx.length; i++) avcodec.avcodec_free_context(codecsCtx[i]);

    avformat.avio_context_free(ioInCtx);
    avformat.avio_context_free(ioOutCtx);
    avformat.avformat_close_input(inFormatCtx);
    avformat.avformat_free_context(inFormatCtx);
    avformat.avformat_free_context(outFormatCtx);
  }

  public JCPPTranscode withFormatName(String formatName) {
    this.outFormat = checkAllocation(AVOutputFormat.class, avformat.av_guess_format(formatName, null, null));
    return this;
  }
}
