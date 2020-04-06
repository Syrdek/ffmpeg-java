package fr.syrdek.ffmpeg.tests.javacpp;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVOutputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
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
public class JCPPTransmux {
  private static final Logger LOG = LoggerFactory.getLogger(JCPPTransmux.class);
  private static final int BUFFER_SIZE = 256 * 1024;

  public static void main(String[] args) throws Exception {
    Utils.cleanup();
    
    try (final InputStream in = new FileInputStream("samples/audio.mp2");
        final OutputStream out = new FileOutputStream("target/result.mkv")) {
      new JCPPTransmux().withFormatName("matroska").transmux(in, out);
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
    inFormatCtx.flags(CFlag.plus(inFormatCtx.flags(), AVFormatFlag.CUSTOM_IO));
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
    outFormatCtx.flags(CFlag.plus(outFormatCtx.flags(), AVFormatFlag.CUSTOM_IO));

    checkAndThrow(avformat.avformat_alloc_output_context2(outFormatCtx, outFormat, (String) null, null));
    outFormatCtx.pb(ioOutCtx);
  }

  /**
   *
   * @param in
   * @throws IOException
   */
  public void transmux(final InputStream in, final OutputStream out) throws IOException {
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
      
      // Enregistre que le flux n°i correspond au flux de sortie outStream.
      streamsMap.put(i, outStream);
    }

    checkAndThrow(avformat.avformat_write_header(outFormatCtx, (AVDictionary)null));

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
      // Recalcule les PTS (presentation timestamp), DTS (decoding timestamp), et la durée de l'image en fonction de la nouvelle base de temps du conteneur.
      // Voir http://dranger.com/ffmpeg/tutorial05.html pour plus d'explications.
      avcodec.av_packet_rescale_ts(packet, inStream.time_base(), outStream.time_base());

      checkAndThrow(avformat.av_interleaved_write_frame(outFormatCtx, packet));
      avcodec.av_packet_unref(packet);
    }

    checkAndThrow(avformat.av_write_trailer(outFormatCtx));

    avformat.avio_context_free(ioInCtx);
    avformat.avio_context_free(ioOutCtx);
    avformat.avformat_close_input(inFormatCtx);
    avformat.avformat_free_context(inFormatCtx);
    avformat.avformat_free_context(outFormatCtx);
  }

  /**
   *
   * @param in
   * @throws IOException
   */
  public void transmux2(final InputStream in, final OutputStream out) throws IOException {
    openInput(in);
    openOutput(out);

    // Table de correspondance entre les flux en entree et en sortie.
    Map<Integer, Integer> streamsMap = new HashMap<Integer, Integer>();

    int outStreamIndex = 0;
    // Parcourt les flux du format.
    for (int i = 0; i < inFormatCtx.nb_streams(); i++) {
      final AVStream inStream = inFormatCtx.streams(i);
      final AVCodecParameters inParams = inStream.codecpar();

      // Ignore les flux qui ne sont ni audio, ni video, ni sous-titre.
      if (inParams.codec_type() != avutil.AVMEDIA_TYPE_AUDIO &&
          inParams.codec_type() != avutil.AVMEDIA_TYPE_VIDEO &&
          inParams.codec_type() != avutil.AVMEDIA_TYPE_SUBTITLE) {
        continue;
      }

      // Ajoute une entrée dans la table de correspondance.
      streamsMap.put(i, outStreamIndex++);

      final AVStream outStream = checkAllocation(avformat.avformat_new_stream(outFormatCtx, null));
      checkAndThrow(avcodec.avcodec_parameters_copy(outStream.codecpar(), inParams));
    }
    avformat.av_dump_format(outFormatCtx, 0, (String) null, 1);

    checkAndThrow(avformat.avformat_write_header(outFormatCtx, (AVDictionary) null));

    final AVPacket packet = new AVPacket();

    while (true) {
      int ret = avformat.av_read_frame(inFormatCtx, packet);
      if (ret == avutil.AVERROR_EAGAIN() || ret == avutil.AVERROR_EOF) {
        // OK, on a fini.
        break;
      }
      // Vérifie si on a une erreur bloquante.
      checkAndThrow(ret);

      int streamInIdx = packet.stream_index();
      final AVStream inStream = inFormatCtx.streams(streamInIdx);

      int streamOutIdx = streamsMap.getOrDefault(streamInIdx, -1);
      if (streamOutIdx < 0) {
        // Le paquet ne fait pas partie d'un flux qui sera transposé.
        avcodec.av_packet_unref(packet);
        continue;
      }
      final AVStream outStream = outFormatCtx.streams(streamOutIdx);
      // Recalcule les PTS (presentation timestamp), DTS (decoding timestamp), et la durée de l'image en fonction de la nouvelle base de temps du conteneur.
      // Voir http://dranger.com/ffmpeg/tutorial05.html pour plus d'explications.
      packet.pts(avutil.av_rescale_q_rnd(packet.pts(), inStream.time_base(), outStream.time_base(), avutil.AV_ROUND_NEAR_INF | avutil.AV_ROUND_PASS_MINMAX));
      packet.dts(avutil.av_rescale_q_rnd(packet.dts(), inStream.time_base(), outStream.time_base(), avutil.AV_ROUND_NEAR_INF | avutil.AV_ROUND_PASS_MINMAX));
      packet.duration(avutil.av_rescale_q(packet.duration(), inStream.time_base(), outStream.time_base()));
      packet.pos(-1); // -1 = inconnu pour laisser libav le calculer.

      checkAndThrow(avformat.av_interleaved_write_frame(outFormatCtx, packet));
      avcodec.av_packet_unref(packet);
    }

    checkAndThrow(avformat.av_write_trailer(outFormatCtx));
    avformat.avformat_close_input(inFormatCtx);

    avformat.avformat_free_context(outFormatCtx);
    avformat.avformat_free_context(inFormatCtx);
    avformat.avio_context_free(ioOutCtx);
    avformat.avio_context_free(ioInCtx);

    // avutil.av_free(outStreamPtr);
    outStreamPtr.deallocate();
    // avutil.av_free(inStreamPtr);
    inStreamPtr.deallocate();
  }

  public JCPPTransmux withFormatName(String formatName) {
    this.outFormat = checkAllocation(AVOutputFormat.class, avformat.av_guess_format(formatName, null, null));
    return this;
  }
}
