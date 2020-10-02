package fr.syrdek.ffmpeg.libav.java;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avdevice;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.postproc;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.javacpp.Loader;
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
      Loader.load(avutil.class);
      Loader.load(avformat.class);
      Loader.load(avcodec.class);
      Loader.load(avdevice.class);
      Loader.load(swresample.class);
      Loader.load(swscale.class);
      Loader.load(postproc.class);

      if (!System.getProperty("org.bytedeco.javacpp.loadlibraries", "true").equalsIgnoreCase("true")) {
        System.loadLibrary("jniavutil");
        System.loadLibrary("jniavformat");
        System.loadLibrary("jniavcodec");
        System.loadLibrary("jniavdevice");
        System.loadLibrary("jniswresample");
        System.loadLibrary("jniswscale");
        System.loadLibrary("jnipostproc");
      }
      loaded = true;
    }
  }
}
