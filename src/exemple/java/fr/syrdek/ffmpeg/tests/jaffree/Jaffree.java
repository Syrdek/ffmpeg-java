package fr.syrdek.ffmpeg.tests.jaffree;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;

import fr.syrdek.ffmpeg.tests.Utils;

/**
 * * AVIS : <br>
 * v Gère les flux.<br>
 * x Mal documenté.<br>
 * x Nécessite une version à jour de ffmpeg.<br>
 * x Flux ouverts vers des ports locaux aléatoires (pas de moyen de les contrôler).
 *
 * @author t0087865
 *
 */
public class Jaffree {
  private static final Logger LOG = LoggerFactory.getLogger(Jaffree.class);

  public static void main(String[] args) {
    final String source = Utils.argOrDefault(args, 0, "src/test/resources/5thElement.mp4");
    final String target = Utils.argOrDefault(args, 1, "target/result.mkv");

    try (
        final InputStream is = new FileInputStream(source);
        final OutputStream os = new FileOutputStream(target);) {

      final ProgressListener listener = progress -> LOG.debug("{} - {}", progress.getTimeMillis(), progress.getSpeed());

      FFmpeg.atPath(Paths.get("/usr/bin"))
          .addInput(PipeInput.pumpFrom(is))
          .addOutput(PipeOutput.pumpTo(os)
              .setCodec(StreamType.AUDIO, "vorbis")
              .setCodec(StreamType.VIDEO, "theora"))
          .setProgressListener(listener)
          .execute();

    } catch (final IOException e) {
      LOG.error("Echec d'ouverture des flux", e);
    }
  }
}
