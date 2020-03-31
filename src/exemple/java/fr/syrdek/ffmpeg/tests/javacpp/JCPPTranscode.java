package fr.syrdek.ffmpeg.tests.javacpp;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.isEOF;

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
import org.bytedeco.javacpp.avutil.AVFrame;
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
        final OutputStream out = new FileOutputStream("target/result.mkv")) {
      new JCPPTranscode()
          .withFormatName("matroska")
//          .withAudioParams(new AudioParameters("mp2", null, null, null, AVSampleFormat.S16))
//           .withAudioParams(new AudioParameters("ac3", null, null, null, AVSampleFormat.FLTP))
           .withAudioParams(new AudioParameters("vorbis", 2, AVChannelLayout.LAYOUT_STEREO.value(), 48000, AVSampleFormat.FLTP))
          // .withVideoParams("libx265", "x265-params", "keyint=60:min-keyint=60:scenecut=0")
          .withMuxerOpt("movflags", "frag_keyframe+empty_moov+default_base_moof")
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
    final int type;

    final int inIndex;
    final AVCodec inCodec;
    final AVStream inStream;
    final AVCodecContext inCodecCtx;
    final AVCodecParameters inCodecPar;

    final int outIndex;
    final AVStream outStream;
    AVCodec outCodec;
    AVCodecContext outCodecCtx;

    BiConsumer<AVPacket, AVFrame> transcoder;
    Consumer<AVFrame> encoder;

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
     * @param <T>
     *          Le type de paramètre à retourner.
     * @param src
     *          La valeur a retourner si non nul.
     * @param def
     *          La valeur a retourner si src est nul.
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
      outCodec = checkAllocation("L'encodeur " + audioParams.getCodec() + " est introuvable", avcodec.avcodec_find_encoder_by_name(audioParams.getCodec()));
      outCodecCtx = checkAllocation(avcodec.avcodec_alloc_context3(outCodec));
      outCodecCtx.channels(getOrDefault(audioParams.getChannels(), inCodecCtx.channels()));
      outCodecCtx.channel_layout(getOrDefault(audioParams.getChannelsLayout(), inCodecCtx.channel_layout()));
      outCodecCtx.sample_rate(getOrDefault(audioParams.getSampleRate(), inCodecCtx.sample_rate()));
      outCodecCtx.sample_fmt(getOrDefault(audioParams.getSampleFormatValue(), inCodecCtx.sample_fmt()));
      AudioParameters.computeTimeBase(outCodecCtx.sample_rate(), outCodecCtx.time_base());

      outCodecCtx.strict_std_compliance(AVEncodingCompliance.EXPERIMENTAL.value());

      outStream.time_base(outCodecCtx.time_base());
      checkAllocation("Echec d'ouverture du codec audio", avcodec.avcodec_open2(outCodecCtx, outCodec, (AVDictionary) null));
      checkAndThrow(avcodec.avcodec_parameters_from_context(outStream.codecpar(), outCodecCtx));

      if (LOG.isDebugEnabled()) {
        LOG.debug("Encoder parameters : channels={}, channel_layout={}, sample_rate={}, sample_fmt={}, bit_rate={}",
            outCodecCtx.channels(),
            AVChannelLayout.toString(outCodecCtx.channel_layout()),
            outCodecCtx.sample_rate(),
            AVSampleFormat.get(outCodecCtx.sample_fmt()),
            outCodecCtx.bit_rate());

        if (logCodecSupports) {
          StringBuilder b = new StringBuilder("Sample formats supportes par l'encodeur ")
              .append(outCodec.name().getString())
              .append(" '")
              .append(outCodec.long_name().getString())
              .append(" :");

          final IntPointer sampleFormats = outCodec.sample_fmts();
          if (sampleFormats != null) {
            for (int i = 0;; i++) {
              int val = sampleFormats.get(i);
              if (val < 0)
                break;
              b.append("  ").append(AVSampleFormat.get(val));
            }
            LOG.debug(b.toString());
          }

          final IntPointer supportedSamplerates = outCodec.supported_samplerates();
          if (supportedSamplerates != null) {
            b = new StringBuilder("Sample rates supportes par l'encodeur ")
                .append(outCodec.name().getString())
                .append(" '")
                .append(outCodec.long_name().getString())
                .append(" :");

            for (int i = 0;; i++) {
              int val = supportedSamplerates.get(i);
              if (val <= 0)
                break;
              b.append("  ").append(val).append("Hz");
            }
            LOG.debug(b.toString());
          }

          final LongPointer channelLayouts = outCodec.channel_layouts();
          if (channelLayouts != null) {
            b = new StringBuilder("Channel layouts supportes par l'encodeur ")
                .append(outCodec.name().getString())
                .append(" '")
                .append(outCodec.long_name().getString())
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

      encoder = this::encodeAudioFrame;
      transcoder = this::transcode;
    }

    public void initVideoTranscodage() {
      // TODO
      throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    /**
     * Ecrit un paquet sans le transcoder.
     * 
     * @param pkt
     *          Le paquet a écrire.
     */
    public void writeOutPacket(final AVPacket pkt) {
      pkt.stream_index(outIndex);
      avcodec.av_packet_rescale_ts(pkt, inCodecCtx.time_base(), outCodecCtx.time_base());
      checkAndThrow(avformat.av_interleaved_write_frame(outFormatCtx, pkt));
      LOG.debug("Packet wrote !");
    }

    /**
     * Décode et réencode le paquet.
     * 
     * @param pkt
     *          Le paquet.
     * @param frame
     *          Un conteneur temporaire dans lequel sera stocké le paquet décodé.
     */
    public void transcode(final AVPacket pkt, final AVFrame frame) {
      checkAndThrow(avcodec.avcodec_send_packet(inCodecCtx, pkt));
      int ret;

      do {
        ret = avcodec.avcodec_receive_frame(inCodecCtx, frame);
        if (!isEOF(ret)) {
          // Vérifie qu'il n'y a pas eu d'erreur associée au code.
          checkAndThrow(ret);

          // Encode et envoie la frame.
          encoder.accept(frame);
        }
        // Sinon, le code indique la fin de traitement du paquet. on arrête...
      } while (ret >= 0);
    }

    /**
     * Encode une frame audio.
     * 
     * @param frame
     *          La frame à encoder.
     */
    public void encodeAudioFrame(final AVFrame frame) {
      final AVPacket packet = avcodec.av_packet_alloc();
      checkAndThrow(avcodec.avcodec_send_frame(outCodecCtx, frame));

      int ret = 0;
      do {
        ret = avcodec.avcodec_receive_packet(outCodecCtx, packet);
        if (!isEOF(ret)) {
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
      avcodec.avcodec_free_context(inCodecCtx);
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
        info = new StreamInfo(i, inStream);
        if (audioParams != null) {
          info.initAudioTranscodage();
        } else {
          info.initStreamCopy();
        }
        break;
      case avutil.AVMEDIA_TYPE_VIDEO:
        info = new StreamInfo(i, inStream);
        if (videoParams != null) {
          info.initVideoTranscodage();
        } else {
          info.initStreamCopy();
        }
        break;
      default:
        continue;
      }

      infos.put(i, info);
    }

    if (CFlag.isIn(outFormat.flags(), avformat.AVFMT_GLOBALHEADER)) {
      outFormatCtx.flags(CFlag.plus(outFormatCtx.flags(), avcodec.AV_CODEC_FLAG_GLOBAL_HEADER));
    }

    final AVDictionary muxerDict = toAvDictionary(this.muxerOpts);
    checkAndThrow(avformat.avformat_write_header(outFormatCtx, muxerDict));

    for (;;) {
      int ret = avformat.av_read_frame(inFormatCtx, packet);
      if (isEOF(ret)) {
        LOG.debug("Fin de stream atteinte (code={}).", ret);
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

    // Nettoyage.
    if (muxerDict != null) {
      avutil.av_dict_free(muxerDict);
    }

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
