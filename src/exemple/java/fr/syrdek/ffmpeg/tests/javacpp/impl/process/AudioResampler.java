/**
 *
 */
package fr.syrdek.ffmpeg.tests.javacpp.impl.process;

import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAllocation;
import static fr.syrdek.ffmpeg.libav.java.FFmpegException.checkAndThrow;
import static fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils.timestampToHms;
import static fr.syrdek.ffmpeg.tests.javacpp.impl.TimeUtils.timestampToString;

import java.util.function.Consumer;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avutil.AVAudioFifo;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.syrdek.ffmpeg.libav.java.io.stream.AudioParameters;
import fr.syrdek.ffmpeg.tests.javacpp.impl.BaseProducer;
import fr.syrdek.ffmpeg.tests.javacpp.impl.encode.AudioEncoder;

/**
 * Fifo permettant de bufferiser les trames audio pour gérer la conversion entre des codec n'utilisant pas la même
 * taille de frame.
 *
 * @author t0087865
 */
public class AudioResampler extends BaseProducer<AVFrame> implements Consumer<AVFrame> {
  private static final Logger LOG = LoggerFactory.getLogger(AudioResampler.class);

  private final AVCodecContext encoderCtx;
  private final AudioParameters outParams;
  private final AVAudioFifo audioFifo;
  private final AVRational timebase;
  private final SwrContext swrCtx;
  private final String codecName;

  /**
   * Construit un resampler audio.
   *
   * @param inParams
   * @param outParams
   * @param encoder
   */
  public AudioResampler(final AudioParameters inParams, final AudioParameters outParams, final AudioEncoder encoder) {
    timebase = encoder.getCodecCtx().time_base();
    encoderCtx = encoder.getCodecCtx();
    this.outParams = outParams;
    codecName = inParams.getCodecName();

    audioFifo = checkAllocation(
        avutil.av_audio_fifo_alloc(outParams.getSampleFormatValue(), outParams.getChannels(), 1));

    swrCtx = checkAllocation(swresample.swr_alloc_set_opts(null,
        encoderCtx.channel_layout(), encoderCtx.sample_fmt(), encoderCtx.sample_rate(),
        inParams.getChannelsLayout(), inParams.getSampleFormatValue(), inParams.getSampleRate(),
        0, null));

    checkAndThrow(swresample.swr_init(swrCtx));
  }

  @Override
  public void accept(final AVFrame frame) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Resampling d'une frame [{}] (DTS: {} [{}], PTS: {} [{}]).", codecName,
          timestampToString(frame.pkt_dts()), timestampToHms(frame.pkt_dts(), timebase),
          timestampToString(frame.pts()), timestampToHms(frame.pts(), timebase));
    }

    resampleAndPushToFifo(frame);
    pullFromFifoToEncoder(false);
  }

  /**
   * Purge la FIFO et envoie les paquets à l'encodeur.
   */
  public void finish() {
    pullFromFifoToEncoder(true);
  }

  /**
   * Convertit la frame et la pousse dans la fifo. TODO: Beaucoup d'allocations à chaque frame, il faudrait peut-être
   * garder des buffers. Ne pas garder dans le framework.
   *
   * @param frame
   *          La frame à traiter et enregistrer dans la fifo.
   */
  private void resampleAndPushToFifo(final AVFrame frame) {
    // Alloue un tableau de pointeurs sur les données des channels audio à convertir.
    final PointerPointer<BytePointer> convertedInputSamples = new PointerPointer<>(2);
    // Demande à libav d'allouer les buffers pour les samples et de remplir notre tableau avec les adresses de ces
    // buffers.
    checkAndThrow(avutil.av_samples_alloc(convertedInputSamples, null, outParams.getChannels(), frame.nb_samples(),
        outParams.getSampleFormatValue(), 0));
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
   * @param untilEnd
   *          <code>true</code> pour dépiler la fifo jusqu'au bout (c'est-à-dire qu'on aura plus de nouvelle donnée à
   *          écrire, et qu'on peut terminer l'envoi à l'encodeur par une frame incomplète),<br>
   *          <code>false</code> pour ne la dépiler que si suffisamment de données sont prêtes pour être encodées dans
   *          une frame complète.
   */
  private void pullFromFifoToEncoder(final boolean untilEnd) {
    int fifoSize = avutil.av_audio_fifo_size(audioFifo);
    while (
    // Dépile tant qu'on a assez de données pour remplir une frame.
    fifoSize >= encoderCtx.frame_size()
        // Dépile jusqu'au bout.
        || untilEnd && fifoSize > 0) {

      // TODO: allouer la frame de sortie une fois et la réutiliser.
      final AVFrame frame = checkAllocation(avutil.av_frame_alloc());
      frame.nb_samples(encoderCtx.frame_size());
      frame.channel_layout(encoderCtx.channel_layout());
      frame.format(encoderCtx.sample_fmt());
      frame.sample_rate(encoderCtx.sample_rate());

      checkAndThrow(avutil.av_frame_get_buffer(frame, 0));

      checkAndThrow(avutil.av_audio_fifo_read(audioFifo, frame.data(), encoderCtx.frame_size()));

      publish(frame);
      fifoSize = avutil.av_audio_fifo_size(audioFifo);
    }
  }
}
