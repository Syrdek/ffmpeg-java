package fr.syrdek.ffmpeg.tests.bin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traite les données sorties par FFMpeg sur le flux stderr.
 *
 * @author t0087865
 */
public class FFMpegInfoHandler implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(FFMpegInfoHandler.class);

  private final StringBuilder mappingMediaInfo = new StringBuilder();
  private final StringBuilder outputMediaInfo = new StringBuilder();
  private final StringBuilder inputMediaInfo = new StringBuilder();
  private final StringBuilder versionInfo = new StringBuilder();
  private final StringBuilder otherInfo = new StringBuilder();

  private final StringBuilder audioCodec = new StringBuilder();
  private final StringBuilder videoCodec = new StringBuilder();
  private String conversionInfo = "";
  private String summary = "";

  /**
   * Sections d'informations logguées par FFMpeg.
   *
   * @author t0087865
   */
  private enum FFMpegInfoTypeEnum {
    VERSION, MAPPING, OUTPUT, INPUT, CONVERSION, SUMMARY, OTHER
  }

  private FFMpegInfoHandler.FFMpegInfoTypeEnum readingInfo = FFMpegInfoTypeEnum.OTHER;

  private final InputStream is;

  /**
   * @param is
   *          Le flux de sortie FFMpeg à lire. Ce flux est proprement fermé lorsqu'il n'y a plus d'informations à y
   *          lire.
   */
  public FFMpegInfoHandler(final InputStream is) {
    this.is = is;
  }

  @Override
  public synchronized void run() {
    try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String line;
      while ((line = br.readLine()) != null) {
        LOG.debug(line);

        guessSection(line);
        extractSectionInfo(line);
      }
    } catch (IOException e) {
      LOG.error("Erreur lors de l'envoi des données lues au logger.", e);
    }
  }

  /**
   * Affecte les informations de la ligne de log à la section correspondante.
   *
   * @param line
   *          La ligne de log à affecter.
   */
  protected void extractSectionInfo(String line) {
    switch (readingInfo) {
    case VERSION:
      versionInfo.append(line).append('\n');
      break;
    case INPUT:
      inputMediaInfo.append(line).append('\n');
      final String trimmedLine = line.trim();
      if (trimmedLine.startsWith("Stream #")) {
        int audioIndex = trimmedLine.indexOf("Audio: ");
        if (audioIndex > 0) {
          audioCodec.append(trimmedLine.substring(audioIndex + 7)).append('\n');
        } else {
          int videoIndex = trimmedLine.indexOf("Video: ");
          if (videoIndex > 0) {
            videoCodec.append(trimmedLine.substring(videoIndex + 7)).append('\n');
          }
        }
      }
      break;
    case MAPPING:
      mappingMediaInfo.append(line).append('\n');
      break;
    case OUTPUT:
      outputMediaInfo.append(line).append('\n');
      break;
    case CONVERSION:
      conversionInfo = line;
      break;
    case SUMMARY:
      summary = line;
      break;
    case OTHER:
      otherInfo.append(line).append('\n');
      break;
    default:
      break;
    }
  }

  /**
   * Retrouve la section correspondant à la ligne de log de FFMpeg.
   *
   * @param line
   *          La ligne à analyser.
   */
  protected void guessSection(String line) {
    final String lowerLine = line.toLowerCase();
    if (lowerLine.startsWith(" ")) {
      // On a pas changé de section. Ne fait rien.
    } else if (lowerLine.startsWith("input")) {
      readingInfo = FFMpegInfoTypeEnum.INPUT;
    } else if (lowerLine.startsWith("output")) {
      readingInfo = FFMpegInfoTypeEnum.OUTPUT;
    } else if (lowerLine.startsWith("ffmpeg version")) {
      readingInfo = FFMpegInfoTypeEnum.VERSION;
    } else if (lowerLine.startsWith("stream mapping")) {
      readingInfo = FFMpegInfoTypeEnum.MAPPING;
    } else if (lowerLine.contains("time=")) {
      readingInfo = FFMpegInfoTypeEnum.CONVERSION;
    } else if (lowerLine.contains("muxing overhead")) {
      readingInfo = FFMpegInfoTypeEnum.SUMMARY;
    } else {
      // On est entré dans une section inconnue.
      readingInfo = FFMpegInfoTypeEnum.OTHER;
    }
  }

  /**
   * @return the mappingMediaInfo
   */
  public synchronized String getMappingMediaInfo() {
    return mappingMediaInfo.toString();
  }

  /**
   * @return the outputMediaInfo
   */
  public synchronized String getOutputMediaInfo() {
    return outputMediaInfo.toString();
  }

  /**
   * @return the inputMediaInfo
   */
  public synchronized String getInputMediaInfo() {
    return inputMediaInfo.toString();
  }

  /**
   * @return the versionInfo
   */
  public synchronized String getVersionInfo() {
    return versionInfo.toString();
  }

  /**
   * @return the otherInfo
   */
  public synchronized String getOtherInfo() {
    return otherInfo.toString();
  }

  /**
   * @return the conversionInfo
   */
  public synchronized String getConversionInfo() {
    return conversionInfo;
  }

  /**
   * @return the summary
   */
  public synchronized String getSummary() {
    return summary;
  }

  /**
   * @return the audioCodec
   */
  public synchronized String getAudioCodec() {
    return audioCodec.toString();
  }

  /**
   * @return the videoCodec
   */
  public synchronized String getVideoCodec() {
    return videoCodec.toString();
  }
}