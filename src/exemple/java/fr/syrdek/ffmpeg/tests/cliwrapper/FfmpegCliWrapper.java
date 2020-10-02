package fr.syrdek.ffmpeg.tests.cliwrapper;

import java.util.concurrent.TimeUnit;

import fr.syrdek.ffmpeg.tests.Utils;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;

/**
 * AVIS : <br>
 * v Très simple d'utilisation.<br>
 * x Affiche la progression via ffprobe (nécessite d'ouvrir un port local).<br>
 * x Impossible de supporter les streams.<br>
 * x Les paramètres de ligne de commande utilisés sont dépréciés (utilisation de -vcodec au lieu de -v:codec)
 *
 *
 * @author t0087865
 *
 */
public class FfmpegCliWrapper {

  public static final void main(final String[] args) throws Exception {
    final String source = Utils.argOrDefault(args, 0, "src/test/resources/5thElement.mp4");
    final String target = Utils.argOrDefault(args, 1, "target/result.mkv");

    final FFmpeg ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
    final FFprobe ffprobe = new FFprobe("/usr/bin/ffprobe");

    final FFmpegBuilder builder = new FFmpegBuilder()

        .setInput(source)
        .overrideOutputFiles(true)

        .addOutput(target)
        .setFormat("matroska")

        .setAudioChannels(2)
        .setAudioCodec("vorbis")
        .setAudioSampleRate(44100)
        .setAudioBitRate(32768)

        .setVideoCodec("mpeg4")
        .setVideoFrameRate(24, 1)
        .setVideoResolution(640, 480)

        .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
        .done();

    final FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

    final FFmpegProbeResult in = ffprobe.probe(source);

    // Run a one-pass encode
    executor.createJob(builder, new ProgressListener() {
      // Using the FFmpegProbeResult determine the duration of the input
      final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

      @Override
      public void progress(Progress progress) {
        double percentage = progress.out_time_ns / duration_ns;

        // Print out interesting information about the progress
        System.out.println(String.format(
            "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx",
            percentage * 100,
            progress.status,
            progress.frame,
            FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
            progress.fps.doubleValue(),
            progress.speed));
      }
    }).run();
  }

}
