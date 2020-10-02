package fr.syrdek.ffmpeg.tests.javacpp;

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.io.AVChannelLayout;
import fr.syrdek.ffmpeg.libav.java.io.AVPixFormat;
import fr.syrdek.ffmpeg.libav.java.io.AVSampleFormat;
import fr.syrdek.ffmpeg.libav.java.io.SWSInterpolation;
import fr.syrdek.ffmpeg.libav.java.io.stream.AudioParameters;
import fr.syrdek.ffmpeg.libav.java.io.stream.VideoParameters;
import fr.syrdek.ffmpeg.tests.Utils;
import fr.syrdek.ffmpeg.tests.javacpp.impl.Demuxer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.Muxer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.Producer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils;
import fr.syrdek.ffmpeg.tests.javacpp.impl.decode.AudioDecoder;
import fr.syrdek.ffmpeg.tests.javacpp.impl.decode.VideoDecoder;
import fr.syrdek.ffmpeg.tests.javacpp.impl.encode.AudioEncoder;
import fr.syrdek.ffmpeg.tests.javacpp.impl.encode.VideoEncoder;
import fr.syrdek.ffmpeg.tests.javacpp.impl.process.AudioResampler;
import fr.syrdek.ffmpeg.tests.javacpp.impl.process.AudioTimestamper;
import fr.syrdek.ffmpeg.tests.javacpp.impl.process.TimestampScaler;
import fr.syrdek.ffmpeg.tests.javacpp.impl.process.VideoLimiter;
import fr.syrdek.ffmpeg.tests.javacpp.impl.process.VideoRescaler;

public class JCPPTranscode {
  private static final Logger LOG = LoggerFactory.getLogger(JCPPTranscode.class);
  private static final int BUFFER_SIZE = 256 * 1024;

  static {
    // S'assure que les libs natives soient bien chargées.
    FFmpegNatives.ensureLoaded();
  }

  /**
   *
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) {
    Utils.cleanup();

    final String source;
    if (args.length > 0) {
      source = args[0];
    } else {
      source = "src/test/resources/5thElement.mp4";
    }

    new JCPPTranscode(source, "target/result.mkv")
        .transcode();
  }

  private final String inputFile;
  private final String outputFile;

  /**
   * @param inputFile
   *          Fichier a transcoder.
   * @param outputFile
   *          Fichier a écrire.
   */
  public JCPPTranscode(final String inputFile, final String outputFile) {
    this.outputFile = outputFile;
    this.inputFile = inputFile;
  }

  public void transcode() {
    final String formatName = "matroska";

    final VideoParameters videoParams = new VideoParameters.Builder()
        .withCodec(avcodec.AV_CODEC_ID_MPEG4)
        .withBitRate(501000l)
        .withWidth(400)
        .withHeight(300)
        .withFrameRate(24)
        .withPixFormat(AVPixFormat.FMT_YUV420P)
        .build();

    final AudioParameters audioParams = new AudioParameters.Builder()
        .withCodec(avcodec.AV_CODEC_ID_VORBIS)
        .withBitRate(107246l)
        .withChannels(2)
        .withChannelsLayout(AVChannelLayout.LAYOUT_STEREO)
        .withSampleFormat(AVSampleFormat.FLTP)
        .withSampleRate(44100)
        .build();

    try (//
        final Demuxer source = new Demuxer(inputFile);
        final Muxer destination = new Muxer(outputFile, formatName);
        final VideoDecoder videoDecoder = source.openVideoDecoder(null);
        final AudioDecoder audioDecoder = source.openAudioDecoder(null);
        final VideoRescaler videoRescaler = new VideoRescaler(
            videoDecoder.getParameters(),
            videoParams,
            SWSInterpolation.POINT);
        final AudioEncoder audioEncoder = new AudioEncoder(audioParams, destination, null);
        final VideoEncoder videoEncoder = new VideoEncoder(videoParams, destination, null);
        final TimestampScaler videoTimestamper = new TimestampScaler(videoDecoder, videoEncoder)//
    ) {

      /////////// Gestion du flux vidéo. ///////////

      // Limite les FPS provenant de la source.
      final Producer<AVFrame> limiter = VideoLimiter.limitFps(
          videoDecoder,
          videoParams.getFrameRate(),
          videoDecoder.getStream().time_base(),
          videoDecoder.getStream().r_frame_rate());
      // A la sortie du limiteur, envoie les frames au video scaler.
      limiter.sendTo(videoRescaler);
      // Le video scaler envoie les frames à l'encoder.
      videoRescaler.sendTo(videoEncoder);
      // L'encodeur envoie les paquets au timestamp scaler.
      videoEncoder.sendTo(videoTimestamper);
      // Le timestamp scaler envoie les paquets au conteneur final (le fichier de sortie).
      videoTimestamper.sendTo(destination);

      /////////// Gestion du flux audio. ///////////

      final AudioResampler audioResampler = new AudioResampler(audioDecoder.getParameters(), audioParams, audioEncoder);
      final AudioTimestamper audioTimestamper = new AudioTimestamper();
      audioDecoder.sendTo(audioResampler);
      audioResampler.sendTo(audioEncoder);
      audioEncoder.sendTo(audioTimestamper);
      audioTimestamper.sendTo(destination);

      source.dumpFormat();
      destination.dumpFormat();

      destination.writeHeaders();

      // La chaine de transformations est prête, il n'y a plus qu'à lancer la conversion.
      source.read();
      // On a besoin de purger la file pour s'assurer qu'aucun paquet incomplet ne reste en attente.
      audioResampler.finish();

      destination.writeTrailer();
    }

    if (LOG.isInfoEnabled()) {
      LOG.info("Recuperation des informations du fichier construit...");
      try (final Demuxer source = new Demuxer(outputFile);
          final AudioDecoder adec = source.openAudioDecoder(null);
          final VideoDecoder vdec = source.openVideoDecoder(null);) {
        LOG.info("# Audio - TB: {}", TimeUtils.toString(adec.getStream().time_base()));
        LOG.info("# Video - TB: {}", TimeUtils.toString(vdec.getStream().time_base()));

        source.read();
      }
    }
  }
}
