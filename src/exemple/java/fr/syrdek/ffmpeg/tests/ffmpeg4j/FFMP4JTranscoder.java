package fr.syrdek.ffmpeg.tests.ffmpeg4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.bytedeco.javacpp.avformat.AVInputFormat;
import org.bytedeco.javacpp.avformat.AVOutputFormat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.manevolent.ffmpeg4j.AudioFormat;
import com.github.manevolent.ffmpeg4j.FFmpeg;
import com.github.manevolent.ffmpeg4j.FFmpegException;
import com.github.manevolent.ffmpeg4j.FFmpegIO;
import com.github.manevolent.ffmpeg4j.FFmpegInput;
import com.github.manevolent.ffmpeg4j.VideoFormat;
import com.github.manevolent.ffmpeg4j.filter.audio.AudioFilter;
import com.github.manevolent.ffmpeg4j.filter.audio.AudioFilterNone;
import com.github.manevolent.ffmpeg4j.filter.audio.FFmpegAudioResampleFilter;
import com.github.manevolent.ffmpeg4j.filter.video.FFmpegVideoRescaleFilter;
import com.github.manevolent.ffmpeg4j.filter.video.VideoFilter;
import com.github.manevolent.ffmpeg4j.filter.video.VideoFilterNone;
import com.github.manevolent.ffmpeg4j.source.AudioSourceSubstream;
import com.github.manevolent.ffmpeg4j.source.VideoSourceSubstream;
import com.github.manevolent.ffmpeg4j.stream.output.FFmpegTargetStream;
import com.github.manevolent.ffmpeg4j.stream.source.FFmpegSourceStream;

import fr.syrdek.ffmpeg.tests.jav.JAVDecoder;

/**
 * 
 * @author Syrdek
 *
 */
public class FFMP4JTranscoder {
  private static final Logger LOG = LoggerFactory.getLogger(FFMP4JTranscoder.class);

  
  public static void main(String[] args) throws Exception {
    try (final InputStream in = new FileInputStream("samples/video.mp4");
        final OutputStream out = new FileOutputStream("target/result.mp4")) {
      new FFMP4JTranscoder().transcode(in, out);
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }
  
  
  private AudioFormat audioFormat;
  
  public FFMP4JTranscoder() {
    this(new AudioFormat(44100, 2, avutil.AV_CH_LAYOUT_STEREO));
  }
  
  public FFMP4JTranscoder(final AudioFormat audioFormat) {
    this.audioFormat = audioFormat;
  }

  public void transcode(final InputStream is, final OutputStream os) throws Exception {
    try {
      final FFmpegIO ffioin = FFmpegIO.openInputStream(is, FFmpegIO.DEFAULT_BUFFER_SIZE);
      final FFmpegIO ffioout = FFmpegIO.openOutputStream(os, FFmpegIO.DEFAULT_BUFFER_SIZE);
      
      final AVInputFormat avFormat = FFmpeg.getInputFormatByName("mp3");
      final FFmpegInput ffin = new FFmpegInput(ffioin);
      
      final FFmpegSourceStream sourceStream = ffin.open(avFormat);
      sourceStream.registerStreams();
      
      final AudioSourceSubstream audioStream = getAudioStream(sourceStream);

      final FFmpegTargetStream targetStream = new FFmpegTargetStream(
          // "matroska", -> @main: ffmpeg/avcodec_send_frame: Invalid argument (code=-22)
          "mp2",
          ffioout,
          new FFmpegTargetStream.FFmpegNativeOutput());
      targetStream.registerAudioSubstream("mp2", audioFormat, new HashMap<String, String>());

      if (targetStream.getSubstreams().size() <= 0)
        throw new FFmpegException("No substreams to convert");
      
      com.github.manevolent.ffmpeg4j.transcoder.Transcoder.convert(sourceStream, targetStream, new AudioFilterNone(), new VideoFilterNone(), 1.0);
      ffin.close();
    } finally {
      os.close();
    }
  }
  
  public static AudioSourceSubstream getAudioStream(FFmpegSourceStream source) {
    return (AudioSourceSubstream) source
        .getSubstreams()
        .stream()
        .filter(x -> x instanceof AudioSourceSubstream)
        .findFirst()
        .orElse(null);
  }
  
  public static VideoSourceSubstream getVideoStream(FFmpegSourceStream source) {
    return (VideoSourceSubstream) source
        .getSubstreams()
        .stream()
        .filter(x -> x instanceof VideoSourceSubstream)
        .findFirst()
        .orElse(null);
  }
  
  public static AVInputFormat guessFormat(final FFmpegInput ffin) {
    return ffin.getFormatContext().iformat();
  }
}
