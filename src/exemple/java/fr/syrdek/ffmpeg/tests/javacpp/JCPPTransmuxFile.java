package fr.syrdek.ffmpeg.tests.javacpp;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.tests.Utils;

/**
 *
 * @author Syrdek
 */
public class JCPPTransmuxFile {
  private static final Logger LOG = LoggerFactory.getLogger(JCPPTransmuxFile.class);
  private static final int BUFFER_SIZE = 256 * 1024;

  public static void main(String[] args) throws Exception {
    Utils.cleanup();

    new JCPPTransmuxFile().withFormatName("matroska").transmux("src/test/resources/5thElement.mp4",
        "target/result.mkv");
  }

  private AVFormatContext inFormatCtx;

  private AVFormatContext outFormatCtx;
  private AVOutputFormat outFormat;

  static {
    // S'assure que les libs natives soient bien chargées.
    FFmpegNatives.ensureLoaded();
  }

  private void openInput(final String in) {
    inFormatCtx = checkAllocation(AVFormatContext.class, avformat.avformat_alloc_context());

    // Ouvre le flux et lit les entêtes.
    checkAndThrow(avformat.avformat_open_input(inFormatCtx, in, null, null));

    // Récupère les informations du format.
    checkAndThrow(avformat.avformat_find_stream_info(inFormatCtx, (AVDictionary) null));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Flux FFMPEG ouvert. Format={}, durée={}s, flux={}",
          inFormatCtx.iformat().long_name().getString(),
          inFormatCtx.duration() / 1000000l,
          inFormatCtx.nb_streams());
    }
  }

  private void openOutput(final String out) {
    outFormatCtx = new AVFormatContext();
    AVIOContext ioCtx = new AVIOContext();

    checkAndThrow(avformat.avformat_alloc_output_context2(outFormatCtx, outFormat, null, out));
    checkAndThrow(avformat.avio_open(ioCtx, out, avformat.AVIO_FLAG_WRITE));
    outFormatCtx.pb(ioCtx);
  }

  /**
   *
   * @param in
   * @throws IOException
   */
  public void transmux(final String in, final String out) throws IOException {
    openInput(in);
    openOutput(out);

    final int nbInStreams = inFormatCtx.nb_streams();

    final AVPacket packet = new AVPacket();
    // Table de correspondance entre les flux en entree et en sortie.
    final Map<Integer, AVStream> streamsMap = new HashMap<>(nbInStreams);

    for (int i = 0; i < nbInStreams; i++) {
      final AVStream inStream = inFormatCtx.streams(i);
      final AVCodecParameters codecPar = inStream.codecpar();

      if (codecPar.codec_type() != avutil.AVMEDIA_TYPE_AUDIO &&
          codecPar.codec_type() != avutil.AVMEDIA_TYPE_VIDEO &&
          codecPar.codec_type() != avutil.AVMEDIA_TYPE_SUBTITLE) {
        LOG.debug("Filtrage du flux de type {}", codecPar.codec_type());
        continue;
      }

      final AVStream outStream = checkAllocation(avformat.avformat_new_stream(outFormatCtx, null));
      checkAndThrow(avcodec.avcodec_parameters_copy(outStream.codecpar(), codecPar));
      // Efface le tag du codec qui est lié à son format d'entrée.
      outStream.codecpar().codec_tag(0);

      // Enregistre que le flux n°i correspond au flux de sortie stream.
      streamsMap.put(i, outStream);
    }

    checkAndThrow(avformat.avformat_write_header(outFormatCtx, (AVDictionary) null));

    for (;;) {
      int ret = avformat.av_read_frame(inFormatCtx, packet);
      if (ret == avutil.AVERROR_EOF) {
        LOG.debug("Fin de stream atteinte (code={}).", ret);
        break;
      }
      // Gestion des autres cas d'erreur.
      checkAndThrow(ret);

      final AVStream inStream = inFormatCtx.streams(packet.stream_index());
      final AVStream outStream = streamsMap.get(packet.stream_index());

      if (outStream == null) {
        avcodec.av_packet_unref(packet);
        continue;
      }

      packet.stream_index(outStream.index());
      // Recalcule les PTS (presentation timestamp), DTS (decoding timestamp), et la durée de l'image en fonction de la
      // nouvelle base de temps du conteneur.
      // Voir http://dranger.com/ffmpeg/tutorial05.html pour plus d'explications.
      avcodec.av_packet_rescale_ts(packet, inStream.time_base(), outStream.time_base());

      checkAndThrow(avformat.av_interleaved_write_frame(outFormatCtx, packet));
      avcodec.av_packet_unref(packet);
    }

    checkAndThrow(avformat.av_write_trailer(outFormatCtx));

    avformat.avformat_close_input(inFormatCtx);
    avformat.avformat_free_context(inFormatCtx);
    avformat.avformat_free_context(outFormatCtx);
  }

  public JCPPTransmuxFile withFormatName(String formatName) {
    outFormat = checkAllocation(AVOutputFormat.class, avformat.av_guess_format(formatName, null, null));
    return this;
  }
}
