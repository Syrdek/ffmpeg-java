package fr.syrdek.ffmpeg.tests.jav;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.FFmpegNatives;
import fr.syrdek.ffmpeg.libav.java.Media;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVInputContainer;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVOutputContainer;
import fr.syrdek.ffmpeg.libav.java.io.container.JAVPacket;
import fr.syrdek.ffmpeg.libav.java.io.stream.in.JAVInputStream;
import fr.syrdek.ffmpeg.libav.java.io.stream.out.JAVOutputStream;
import fr.syrdek.ffmpeg.tests.Utils;

/**
 * 
 * @author Syrdek
 */
public class JAVTransmux {
  private static final Logger LOG = LoggerFactory.getLogger(JAVTransmux.class);

  static {
    // S'assure que les libs natives soient bien chargées.
    FFmpegNatives.ensureLoaded();
  }
  
  public static void main(String[] args) throws Exception {
    try (final InputStream in = new FileInputStream("samples/video.mp4");
        final OutputStream out = new FileOutputStream("target/result.mkv")) {
      new JAVTransmux().transmux(in, out, "video/x-matroska");
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }
  
  public void transmux(final InputStream in, final OutputStream out, final String mimeType) throws IOException {
    try (
        // Conteneur source.
        final JAVInputContainer inContainer = new JAVInputContainer.Builder()
          .build(in);
        // Conteneur de destination.
        final JAVOutputContainer outContainer = new JAVOutputContainer.Builder()
            .withFormatMimeType(mimeType)
            .build(out)) {
      
      final Map<JAVInputStream, JAVOutputStream> streamMaps = new HashMap<>();
      final List<Media> acceptedMedia = Arrays.asList(Media.AUDIO, Media.VIDEO, Media.SUBTITLE);
      
      // Crée un flux en sortie pour chaque flux en entrée.
      inContainer.getStreams().stream().filter(s->acceptedMedia.contains(s.getMedia())).forEach(s -> {
          final JAVOutputStream outStream = outContainer.addStream();
          outStream.copyCodecParams(s);
          streamMaps.put(s, outStream);
      });
      
      // Ecrite les entêtes.
      outContainer.writeHeaders();
      
      JAVPacket packet = null;
      // Transfert chaque paquet de donnée provenant du flux en entrée vers le flux en sortie correspondant/
      while ((packet = inContainer.readPacket()) != null) {
        final JAVOutputStream outStream = streamMaps.get(packet.getOrigin());
        if (outStream != null) {
          outStream.writeEncodedPacket(packet);
        }
      }
      
      // Ecrit le pied du fichier.
      outContainer.writeTrailer();
    }
  }
}
