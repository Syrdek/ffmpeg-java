package fr.syrdek.ffmpeg.libav.java;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avdevice;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.postproc;
import org.bytedeco.javacpp.presets.swresample;
import org.bytedeco.javacpp.presets.swscale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Syrdek
 */
public class FFmpegNatives {
  private static final Logger LOG = LoggerFactory.getLogger(FFmpegNatives.class);

  private static volatile boolean loaded = false;

  /**
   * Charge les librairies natives.
   */
  public synchronized static final void ensureLoaded() {
    if (!loaded) {
      // Chargement de librairies natives.
      LOG.debug("Chargement des librairies natives.");
      Loader.load(avformat.class);
      Loader.load(avcodec.class);
      Loader.load(avutil.class);
      Loader.load(avdevice.class);
      Loader.load(swresample.class);
      Loader.load(swscale.class);
      Loader.load(postproc.class);
      loaded = true;
    }
  }
}
