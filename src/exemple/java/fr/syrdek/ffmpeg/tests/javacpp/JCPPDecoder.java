package fr.syrdek.ffmpeg.tests.javacpp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVCodecParserContext;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.manevolent.ffmpeg4j.FFmpegException;

import fr.syrdek.ffmpeg.tests.Utils;

/**
 * 
 * @author Syrdek
 *
 */
public class JCPPDecoder {
  private static final Logger LOG = LoggerFactory.getLogger(JCPPDecoder.class);


  public static void main(String[] args) throws Exception {
    Utils.cleanup();
    
    try (final InputStream in = new FileInputStream("samples/audio.mp2");
        final OutputStream out = new FileOutputStream("target/result.wav")) {
      new JCPPDecoder(in, out).decode();
    } catch (Exception e) {
      LOG.error("Erreur dans le main", e);
    }
  }

  private static final int AUDIO_INBUF_SIZE = 20480;
  private static final int ARRAY_REFILL_THRESHOLD = 4096;

  private final WritableByteChannel outfile;
  private final ReadableByteChannel infile;
  
  public JCPPDecoder(final InputStream infile, final OutputStream outfile) {
    super();
    this.infile = Channels.newChannel(infile);
    this.outfile = Channels.newChannel(outfile);
  }

  /**
   * Transcode le flux.
   * 
   * @throws Exception
   */
  public void decode() throws Exception {
    LOG.debug("Audio decoding");

    AVPacket packet = avcodec.av_packet_alloc();
    AVCodec codec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_MP2);

    if (codec == null) {
      throw new FFmpegException("Impossible de trouver le codec");
    }

    AVCodecParserContext parser = avcodec.av_parser_init(codec.id());
    if (parser == null) {
      throw new FFmpegException("Impossible de construire le parseur");
    }

    AVCodecContext codecContext = avcodec.avcodec_alloc_context3(codec);
    if (codecContext == null) {
      throw new FFmpegException("Impossible d'allouer le contexte de codec audio");
    }

    if (avcodec.avcodec_open2(codecContext, codec, (AVDictionary) null) < 0) {
      throw new FFmpegException("Impossible d'ouvrir le codec");
    }

    BytePointer dataPtr = new BytePointer(AUDIO_INBUF_SIZE);
    ByteBuffer dataBuffer = dataPtr.asBuffer();

    int dataSize = infile.read(dataBuffer);
    int dataOffset = 0;

    boolean finishedReading = false;
    final BytePointer packetDataPtr = new BytePointer();
    AVFrame decodedFrame = null;

    IntPointer packetSizePtr = new IntPointer(1);
    packetSizePtr.put(packet.size());

    long totalread = dataSize;
    long totaldecoded = 0l;

    while (dataSize > 0) {
      if (decodedFrame == null) {
        decodedFrame = avutil.av_frame_alloc();
        if (decodedFrame == null) {
          throw new FFmpegException("Impossible d'allouer la frame audio");
        }
      }

      int ret = avcodec.av_parser_parse2(
          parser,
          codecContext,
          packetDataPtr,
          packetSizePtr,
          dataPtr,
          dataSize,
          avutil.AV_NOPTS_VALUE,
          avutil.AV_NOPTS_VALUE,
          0l);

      if (ret < 0) {
        throw new FFmpegException("Echec lors de l'analyse du paquet");
      }

      packet.data(packetDataPtr);
      packet.size(packetSizePtr.get(0));

      // Repositionne le pointeur sur les prochaines données non traitées.
      dataOffset += ret;
      dataSize -= ret;
      dataPtr = dataPtr.position(dataOffset);
      LOG.debug("Buffer position: {} offset: {} remaining {}", dataPtr.position(), dataOffset, dataSize);

      if (packet.size() > 0) {
        totaldecoded += packet.size();
        decodePacket(codecContext, packet, decodedFrame);
      }

      if (!finishedReading && ARRAY_REFILL_THRESHOLD > dataSize) {
        // On prépare un nouveau buffer.
        BytePointer newDataPtr = new BytePointer(AUDIO_INBUF_SIZE);
        ByteBuffer newBuffer = newDataPtr.asBuffer();

        // On recopie les données du buffer courant au début du nouveau buffer.
        dataBuffer.position(AUDIO_INBUF_SIZE - dataSize);
        newBuffer.position(0);
        newBuffer.put(dataBuffer);

        // On finit de remplir le buffer avec les données lues depuis l'entrée.
        int nbRead = infile.read(newBuffer);

        if (nbRead >= 0) {
          // On se trouve actuellement à la fin des données actuellement disponibles.
          dataSize = newBuffer.position();
          newBuffer.position(0);
          dataOffset = 0;

          dataPtr.close();

          // On passe sur le nouveau buffer.
          dataBuffer = newBuffer;
          dataPtr = newDataPtr;

          newDataPtr.close();
          LOG.debug("Remplissage du buffer ({} octets). Nouvelle taille: {}", nbRead, dataSize);
          totalread += nbRead;
        } else {
          LOG.debug("Le flux audio a été entièrement lu.");
          finishedReading = true;
        }
      }
    }

    long remaining = (totalread - totaldecoded);
    if (remaining > 0 && LOG.isWarnEnabled()) {
      LOG.warn("Octets lus: {}, décodés: {}, restant: {}",
          totalread,
          totaldecoded,
          remaining);
    } else if (LOG.isDebugEnabled()) {
      LOG.debug("Octets lus et décodés: {}",
          totalread,
          totaldecoded);
    }
    
    dataPtr.close();
  }

  /**
   * Décode un paquet.
   * 
   * @param decContext
   *          Le codec utilisé.
   * @param packet
   *          Le paquet à décoder.
   * @param frame
   *          La frame a remplir.
   * @throws FFmpegException
   *           Si le décodage à échoué.
   * @throws IOException 
   */
  private void decodePacket(final AVCodecContext decContext, final AVPacket packet, final AVFrame frame) throws FFmpegException, IOException {
    LOG.debug("Decodage d'un paquet size: {}", packet.size());
    int ret = avcodec.avcodec_send_packet(decContext, packet);

    if (ret < 0) {
      throw new FFmpegException("Impossible d'envoyer le paquet au décodeur");
    }
    
    while (ret >= 0) {
      ret = avcodec.avcodec_receive_frame(decContext, frame);
      if (ret == avutil.AVERROR_EAGAIN() || ret == avutil.AVERROR_EOF) {
        LOG.debug("Fin de décodage du paquet");
        return;
      } else if(ret < 0) {
        throw new FFmpegException("Erreur irrécupérable lors du décodage de frame");
      }
      
      int dataSize = avutil.av_get_bytes_per_sample(decContext.sample_fmt());
      if (dataSize < 0) {
        throw new FFmpegException("Impossible de calculer la taille de l'échantillon");
      }

      LOG.debug("Ecriture de {} samples. Channels: {}, size: {}", frame.nb_samples(), decContext.channels(), dataSize);
      
      for (int i = 0; i < frame.nb_samples(); i++) {
        for (int channel = 0; channel < decContext.channels(); channel++) {
          final ByteBuffer frameData = frame.data(channel)
              .position(i*dataSize)
              .limit((i+1)*dataSize)
              .asByteBuffer();
          outfile.write(frameData);
        }
      }
    }
  }
}
