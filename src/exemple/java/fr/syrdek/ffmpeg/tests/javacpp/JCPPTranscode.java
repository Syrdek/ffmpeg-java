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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.PointerPointer;
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
import org.bytedeco.javacpp.avutil.AVAudioFifo;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacpp.swresample;
import org.bytedeco.javacpp.swresample.SwrContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.CFlag;
import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.AVChannelLayout;
import fr.syrdek.ffmpeg.libav.java.io.AVEncodingCompliance;
import fr.syrdek.ffmpeg.libav.java.io.AVFormatFlag;
import fr.syrdek.ffmpeg.libav.java.io.AVSampleFormat;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVOutputContainer;
import fr.syrdek.ffmpeg.libav.java.io.stream.AudioParameters;
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

    try (final InputStream in = new FileInputStream("samples/video.mp4");
        final OutputStream out = new FileOutputStream("target/result.ogg")) {
      new JCPPTranscode()
          .withFormatName("ogg")
          // .withFormatName("matroska")
          // .withAudioParams(new AudioParameters("mp2", null, null, null, AVSampleFormat.S16))
          // .withAudioParams(new AudioParameters("ac3", null, null, null, AVSampleFormat.FLTP))
          // .withAudioParams(new AudioParameters("aac", 2, AVChannelLayout.LAYOUT_STEREO.value(), 48000,
          // AVSampleFormat.FLTP))
          .withAudioParams(new AudioParameters("vorbis", 2, AVChannelLayout.LAYOUT_STEREO.value(), 48000, AVSampleFormat.FLTP))
          // .withVideoParams("theora")
          // .withMuxerOpt("movflags", "frag_keyframe+empty_moov+default_base_moof")
          .transcode(in, out);
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }

  private boolean logCodecSupports = true;

  // Paramètres en entrée.
  private AVFormatContext inFormatCtx;
  private BytePointer inStreamPtr;
  private AVIOContext ioInCtx;

  // Paramètres en sortie.
  // Format
  private AVFormatContext outFormatCtx;
  private AVOutputFormat outFormat;
  private BytePointer outStreamPtr;
  private AVIOContext ioOutCtx;

  // Options de format
  private Map<String, String> muxerOpts = new HashMap<>();

  // Sortie audio.
  private AudioParameters audioParams;

  // Sortie vidéo.
  private Object videoParams;

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

  class StreamInfo implements AutoCloseable {
    // En entrée.
    final int inIndex;
    final AVCodec inCodec;
    final AVStream inStream;
    final AVCodecContext inCodecCtx;
    final AVCodecParameters inCodecPar;

    // En sortie.
    final int outIndex;
    final AVStream outStream;
    AVCodec outCodec;
    AVCodecContext outCodecCtx;
    int outPts = 0;

    // Outils de convertion.
    BiConsumer<AVPacket, AVFrame> transcoder;
    Consumer<AVFrame> encoder;

    final int type;
    // Tampon de convertion pour gérer les formats de sortie ayant une taille de frame
    // différente du format d'entrée.
    private AVAudioFifo audioFifo;
    private SwrContext swrCtx;

    public StreamInfo(int index, AVStream inStream) {
      this.inCodecPar = inStream.codecpar();
      this.type = this.inCodecPar.codec_type();

      this.inIndex = index;
      this.inStream = inStream;
      this.inCodec = checkAllocation(avcodec.avcodec_find_decoder(this.inCodecPar.codec_id()));
      this.inCodecCtx = checkAllocation(avcodec.avcodec_alloc_context3(this.inCodec));
      checkAndThrow(avcodec.avcodec_parameters_to_context(this.inCodecCtx, this.inCodecPar));
      checkAndThrow(avcodec.avcodec_open2(this.inCodecCtx, this.inCodec, (AVDictionary) null));

      this.outStream = checkAllocation(avformat.avformat_new_stream(outFormatCtx, null));
      this.outIndex = outStream.index();
    }

    /**
     * Retourne src si non nul, sinon, retourne def.
     * 
     * @param <T> Le type de paramètre à retourner.
     * @param src La valeur a retourner si non nul.
     * @param def La valeur a retourner si src est nul.
     * @return La valeur.
     */
    public <T> T getOrDefault(T src, T def) {
      if (null == src)
        return def;
      return src;
    }

    public void initStreamCopy() {
      checkAndThrow(avcodec.avcodec_parameters_copy(outStream.codecpar(), this.inCodecPar));
      transcoder = (pkt, frame) -> writeOutPacket(pkt);
    }

    public void initAudioTranscodage() {
      outCodec = checkAllocation("L'encodeur " + audioParams.getCodec() + " est introuvable",
          avcodec.avcodec_find_encoder_by_name(audioParams.getCodec()));
      outCodecCtx = checkAllocation(avcodec.avcodec_alloc_context3(outCodec));
      outCodecCtx.channels(getOrDefault(audioParams.getChannels(), inCodecCtx.channels()));
      outCodecCtx.channel_layout(getOrDefault(audioParams.getChannelsLayout(), inCodecCtx.channel_layout()));
      outCodecCtx.sample_rate(getOrDefault(audioParams.getSampleRate(), inCodecCtx.sample_rate()));
      outCodecCtx.sample_fmt(getOrDefault(audioParams.getSampleFormatValue(), inCodecCtx.sample_fmt()));
      AudioParameters.computeTimeBase(outCodecCtx.sample_rate(), outCodecCtx.time_base());

      outCodecCtx.strict_std_compliance(AVEncodingCompliance.EXPERIMENTAL.value());

      outStream.time_base(outCodecCtx.time_base());
      checkAllocation("Echec d'ouverture du codec audio",
          avcodec.avcodec_open2(outCodecCtx, outCodec, (AVDictionary) null));
      checkAndThrow(avcodec.avcodec_parameters_from_context(outStream.codecpar(), outCodecCtx));

      if (LOG.isDebugEnabled()) {
        LOG.debug("Encoder parameters : channels={}, channel_layout={}, sample_rate={}, sample_fmt={}, bit_rate={}",
            outCodecCtx.channels(),
            AVChannelLayout.toString(outCodecCtx.channel_layout()),
            outCodecCtx.sample_rate(),
            AVSampleFormat.get(outCodecCtx.sample_fmt()),
            outCodecCtx.bit_rate());

        if (logCodecSupports) {
          logAudioCodecSupports(outCodec);
        }
      }

      encoder = this::encodeAudioFrame;
      if (!shouldResample()) {
        // Faut-t-il réellement conserver ce cas qui a très peu de chances de se produire ?
        transcoder = this::transcode;
      } else {
        if (inCodecCtx.sample_rate() != outCodecCtx.sample_rate()) {
          // TODO
          throw new UnsupportedOperationException(
              "Le resampling avec des sample rate différents n'est pas encore géré.");
        }

        // Le transcoding passera par une phase de resampling.
        audioFifo = checkAllocation(avutil.av_audio_fifo_alloc(outCodecCtx.sample_fmt(), outCodecCtx.channels(), 1));
        swrCtx = checkAllocation(swresample.swr_alloc_set_opts(null,
            outCodecCtx.channel_layout(), outCodecCtx.sample_fmt(), outCodecCtx.sample_rate(),
            inCodecCtx.channel_layout(), inCodecCtx.sample_fmt(), inCodecCtx.sample_rate(),
            0, null));

        checkAndThrow(swresample.swr_init(swrCtx));
        transcoder = this::resampleAndTranscode;
      }
    }

    public void initVideoTranscodage() {
      // TODO
      throw new UnsupportedOperationException("Le transcodage vidéo n'est pas implémenté.");
    }

    public boolean shouldResample() {
      return inCodecCtx.frame_size() != outCodecCtx.frame_size()
          || inCodecCtx.channels() != outCodecCtx.channels()
          || inCodecCtx.channel_layout() != outCodecCtx.channel_layout()
          || inCodecCtx.sample_fmt() != outCodecCtx.sample_fmt()
          || inCodecCtx.sample_rate() != outCodecCtx.sample_rate();
    }

    /**
     * Ecrit un paquet sans le transcoder.
     * 
     * @param pkt Le paquet a écrire.
     */
    public void writeOutPacket(final AVPacket pkt) {
      pkt.stream_index(outIndex);
      // Recalcule les PTS (presentation timestamp), DTS (decoding timestamp), et la durée de l'image en fonction de la
      // nouvelle base de temps du conteneur.
      // Voir http://dranger.com/ffmpeg/tutorial05.html pour plus d'explications.
      avcodec.av_packet_rescale_ts(pkt, inStream.time_base(), outStream.time_base());
      checkAndThrow(avformat.av_interleaved_write_frame(outFormatCtx, pkt));
    }

    /**
     * Décode et réencode, paquet par paquet.
     * 
     * @param pkt   Le paquet.
     * @param frame Un conteneur temporaire dans lequel sera stocké le paquet décodé.
     */
    public void transcode(final AVPacket pkt, final AVFrame frame) {
      checkAndThrow(avcodec.avcodec_send_packet(inCodecCtx, pkt));
      int ret;

      do {
        ret = avcodec.avcodec_receive_frame(inCodecCtx, frame);
        if (ret != avutil.AVERROR_EOF && ret != avutil.AVERROR_EAGAIN()) {
          // Vérifie qu'il n'y a pas eu d'erreur associée au code.
          checkAndThrow(ret);

          // Encode et envoie la frame.
          encoder.accept(frame);
        }
        // Sinon, le code indique la fin de traitement du paquet. on arrête...
      } while (ret >= 0);
    }

    /**
     * Décode et enregistre les paquets dans la fifo, jusqu'à avoir assez de donnée pour les encoder.
     * 
     * @param pkt   Le paquet.
     * @param frame Un conteneur temporaire dans lequel sera stocké le paquet décodé.
     */
    public void resampleAndTranscode(final AVPacket pkt, final AVFrame frame) {
      int ret = 0;

      checkAndThrow(avcodec.avcodec_send_packet(inCodecCtx, pkt));

      do {
        // On remplit la fifo avec les données décodées.
        ret = avcodec.avcodec_receive_frame(inCodecCtx, frame);
        if (ret == avutil.AVERROR_EAGAIN()) {
          // On a besoin de plus de données pour décoder une frame.
          break;
        }
        if (ret == avutil.AVERROR_EOF) {
          // Le flux a été entièrement traité.
          // Envoie toutes les données restantes dans la fifo à l'encodeur.
          pullFromFifoToEncoder(true);
          return;
        }

        // Vérifie qu'il n'y a pas eu d'erreur associée au code.
        checkAndThrow(ret);

        resampleAndPushToFifo(frame);
      } while (ret >= 0);

      // Traite toutes les données qui sont prêtes à être encodées.
      pullFromFifoToEncoder(false);
    }

    /**
     * Convertit la frame et la pousse dans la fifo. TODO: Beaucoup d'allocations à chaque frame, il faudrait peut-être garder des buffers. Ne pas garder dans le framework.
     * 
     * @param frame La frame à traiter et enregistrer dans la fifo.
     */
    private void resampleAndPushToFifo(final AVFrame frame) {
      // Tableau de pointeurs sur les données des channels audio à convertir.
      final PointerPointer<BytePointer> convertedInputSamples = new PointerPointer<>(2);
      // Demande à libav d'allouer les buffers pour les samples et de remplir notre tableau avec les adresses de ces
      // buffers.
      checkAndThrow(avutil.av_samples_alloc(convertedInputSamples, null, outCodecCtx.channels(), frame.nb_samples(),
          outCodecCtx.sample_fmt(), 0));
      // Convertit les frames au format cible.
      checkAndThrow(swresample.swr_convert(swrCtx, convertedInputSamples, frame.nb_samples(), frame.extended_data(),
          frame.nb_samples()));

      // Aggrandit la fifo pour contenir les nouveaux samples.
      avutil.av_audio_fifo_realloc(audioFifo, avutil.av_audio_fifo_size(audioFifo) + frame.nb_samples());
      // Ecrit la nouvelle donnée dans la fifo.
      avutil.av_audio_fifo_write(audioFifo, convertedInputSamples, frame.nb_samples());

      // Nettoie les buffers temporaires.
      avutil.av_freep(convertedInputSamples);
      convertedInputSamples.deallocate();
    }

    /**
     * Dépile les frames depuis la fifo, et les envoie à l'encodeur.
     * 
     * @param untilEnd <code>true</code> pour dépiler la fifo jusqu'au bout (c'est-à-dire qu'on aura plus de nouvelle donnée à écrire, et qu'on peut terminer l'envoi à l'encodeur
     *                 par une frame incomplète),<br>
     *                 <code>false</code> pour ne la dépiler que si suffisamment de données sont prêtes pour être encodées dans une frame complète.
     */
    private void pullFromFifoToEncoder(final boolean untilEnd) {
      int fifoSize = avutil.av_audio_fifo_size(audioFifo);
      while (
      // Dépile tant qu'on a assez de données pour remplir une frame.
      fifoSize >= outCodecCtx.frame_size()
          // Dépile jusqu'au bout.
          || untilEnd && fifoSize > 0) {

        // TODO: allouer la frame de sortie une fois et la réutiliser.
        final AVFrame frame = checkAllocation(avutil.av_frame_alloc());
        frame.nb_samples(outCodecCtx.frame_size());
        frame.channel_layout(outCodecCtx.channel_layout());
        frame.format(outCodecCtx.sample_fmt());
        frame.sample_rate(outCodecCtx.sample_rate());

        checkAndThrow(avutil.av_frame_get_buffer(frame, 0));

        checkAndThrow(avutil.av_audio_fifo_read(audioFifo, frame.data(), outCodecCtx.frame_size()));

        final AVPacket packet = avcodec.av_packet_alloc();
        checkAndThrow(avcodec.avcodec_send_frame(outCodecCtx, frame));

        int ret = 0;
        do {
          ret = avcodec.avcodec_receive_packet(outCodecCtx, packet);
          if (ret != avutil.AVERROR_EOF && ret != avutil.AVERROR_EAGAIN()) {
            // Vérifie qu'il n'y a pas eu d'erreur associée au code.
            checkAndThrow(ret);
            packet.dts(outPts);
            packet.pts(outPts);
            outPts += packet.duration();

            // Ecrit le paquet.
            ret = avformat.av_interleaved_write_frame(outFormatCtx, packet);
            checkAndThrow(ret);
          }
        } while (ret >= 0);

        avcodec.av_packet_unref(packet);
        avcodec.av_packet_free(packet);

        fifoSize = avutil.av_audio_fifo_size(audioFifo);
        avutil.av_frame_free(frame);
      }
    }

    /**
     * Encode une frame audio.
     * 
     * @param frame La frame à encoder.
     */
    public void encodeAudioFrame(final AVFrame frame) {
      final AVPacket packet = avcodec.av_packet_alloc();
      checkAndThrow(avcodec.avcodec_send_frame(outCodecCtx, frame));

      int ret = 0;
      do {
        ret = avcodec.avcodec_receive_packet(outCodecCtx, packet);
        if (ret != avutil.AVERROR_EOF && ret != avutil.AVERROR_EAGAIN()) {
          // Vérifie qu'il n'y a pas eu d'erreur associée au code.
          checkAndThrow(ret);
          // Ecrit le paquet.
          writeOutPacket(packet);
        }
      } while (ret >= 0);

      avcodec.av_packet_unref(packet);
      avcodec.av_packet_free(packet);
    }

    @Override
    public void close() {
      if (swrCtx != null) {
        swresample.swr_free(swrCtx);
        swrCtx = null;
      }

      if (audioFifo != null) {
        avutil.av_audio_fifo_free(audioFifo);
        audioFifo = null;
      }
      avcodec.avcodec_free_context(inCodecCtx);
    }
  }

  private void logAudioCodecSupports(AVCodec codec) {
    if (LOG.isDebugEnabled()) {
      StringBuilder b = new StringBuilder("Sample formats supportes par l'encodeur ")
          .append(codec.name().getString())
          .append(" '")
          .append(codec.long_name().getString())
          .append(" :");

      final IntPointer sampleFormats = codec.sample_fmts();
      if (sampleFormats != null) {
        for (int i = 0;; i++) {
          int val = sampleFormats.get(i);
          if (val < 0)
            break;
          b.append("  ").append(AVSampleFormat.get(val));
        }
        LOG.debug(b.toString());
      }

      final IntPointer supportedSamplerates = codec.supported_samplerates();
      if (supportedSamplerates != null) {
        b = new StringBuilder("Sample rates supportes par l'encodeur ")
            .append(codec.name().getString())
            .append(" '")
            .append(codec.long_name().getString())
            .append(" :");

        for (int i = 0;; i++) {
          int val = supportedSamplerates.get(i);
          if (val <= 0)
            break;
          b.append("  ").append(val).append("Hz");
        }
        LOG.debug(b.toString());
      }

      final LongPointer channelLayouts = codec.channel_layouts();
      if (channelLayouts != null) {
        b = new StringBuilder("Channel layouts supportes par l'encodeur ")
            .append(codec.name().getString())
            .append(" '")
            .append(codec.long_name().getString())
            .append(" :");

        for (int i = 0;; i++) {
          long val = channelLayouts.get(i);
          if (val <= 0)
            break;
          b.append("  ").append(AVChannelLayout.toString(val));
        }
        LOG.debug(b.toString());
      }
    }
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
    final Map<Integer, StreamInfo> infos = new HashMap<Integer, StreamInfo>();

    for (int i = 0; i < nbInStreams; i++) {
      final AVStream inStream = inFormatCtx.streams(i);
      final AVCodecParameters codecPar = inStream.codecpar();

      StreamInfo info = null;
      switch (codecPar.codec_type()) {
      case avutil.AVMEDIA_TYPE_AUDIO:
        if (audioParams != null) {
          info = new StreamInfo(i, inStream);
          info.initAudioTranscodage();
        }
        break;
      case avutil.AVMEDIA_TYPE_VIDEO:
        if (videoParams != null) {
          info = new StreamInfo(i, inStream);
          info.initVideoTranscodage();
        } else {
          info = null;
        }
        break;
      default:
        continue;
      }

      if (info != null)
        infos.put(i, info);
    }

    if (CFlag.isIn(outFormat.flags(), avformat.AVFMT_GLOBALHEADER)) {
      outFormatCtx.flags(CFlag.plus(outFormatCtx.flags(), avcodec.AV_CODEC_FLAG_GLOBAL_HEADER));
    }

    final AVDictionary muxerDict = toAvDictionary(this.muxerOpts);
    checkAndThrow(avformat.avformat_write_header(outFormatCtx, muxerDict));

    final AVFrame frame = avutil.av_frame_alloc();

    for (;;) {
      int ret = avformat.av_read_frame(inFormatCtx, packet);
      if (ret == avutil.AVERROR_EOF || ret == avutil.AVERROR_EAGAIN()) {
        // Fin de stream atteinte ou il faut plus de données pour décoder la prochaine frame.
        break;
      }
      // Gestion des autres cas d'erreur.
      checkAndThrow(ret);

      final StreamInfo info = infos.get(packet.stream_index());
      if (info != null) {
        info.transcoder.accept(packet, frame);
      }

      avcodec.av_packet_unref(packet);
    }

    checkAndThrow(avformat.av_write_trailer(outFormatCtx));

    // Nettoyage.
    if (muxerDict != null) {
      avutil.av_dict_free(muxerDict);
    }

    avutil.av_frame_free(frame);
    infos.values().forEach(StreamInfo::close);
    avformat.avio_context_free(ioInCtx);
    avformat.avio_context_free(ioOutCtx);
    avformat.avformat_close_input(inFormatCtx);
    avformat.avformat_free_context(inFormatCtx);
    avformat.avformat_free_context(outFormatCtx);
  }

  private AVDictionary toAvDictionary(Map<String, String> mop) {
    final AVDictionary avmuxer;
    if (!mop.isEmpty()) {
      avmuxer = new AVDictionary();
      mop.forEach((k, v) -> {
        avutil.av_dict_set(avmuxer, k, v, 0);
      });
    } else {
      avmuxer = null;
    }
    return avmuxer;
  }

  public JCPPTranscode withFormatName(String formatName) {
    this.outFormat = checkAllocation("Format " + formatName + " inconnu", avformat.av_guess_format(formatName, null, null));
    LOG.debug("Format de sortie : {} ({})", outFormat.long_name().getString(), outFormat.name().getString());
    return this;
  }

  public JCPPTranscode withAudioParams(final AudioParameters params) {
    this.audioParams = params;
    return this;
  }

  public JCPPTranscode withMuxerOpt(String key, String val) {
    muxerOpts.put(key, val);
    return this;
  }
}
