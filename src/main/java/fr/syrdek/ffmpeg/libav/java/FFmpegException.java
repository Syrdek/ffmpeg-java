package fr.syrdek.ffmpeg.libav.java;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import org.bytedeco.javacpp.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception lancée lorsqu'une erreur empêche le traitement audio/video par ffmpeg.
 * 
 * @author Syrdek
 */
public class FFmpegException extends RuntimeException {
  private static final Logger LOG = LoggerFactory.getLogger(FFmpegException.class);
  private static final long serialVersionUID = 1L;

  // Longueur max d'un message d'erreur FFmpeg.
  private static final int ERR_MSG_MAX_SIZE = 1024;

  /**
   * Détermine si un code retour FFmpeg est une erreur.
   * 
   * @param returnCode
   *          Lecode retour.
   * @return <code>true</code> si le code indique une erreur, <code>false</code> sinon.
   */
  public static boolean isError(int returnCode) {
    return returnCode < 0;
  }

  /**
   * Récupère l'erreur correspondant au code donné.
   * 
   * @param returnCode
   *          Le code d'erreur.
   * @return Le message de l'erreur, ou <code>null</code> si le code n'est pas une erreur.
   */
  public static String getFFmpegError(int returnCode) {
    if (isError(returnCode)) {
      final ByteBuffer byteBuffer = ByteBuffer.allocate(ERR_MSG_MAX_SIZE);
      avutil.av_strerror(returnCode, byteBuffer, ERR_MSG_MAX_SIZE);

      return MessageFormat.format(
          "{0} (code={1})",
          new String(byteBuffer.array(), Charset.defaultCharset()).trim(),
          returnCode);
    }
    return null;
  }

  /**
   * Détermine si le code libav correspond à une fin de traitement.
   * 
   * @param returnCode
   *          Le code retour de libav.
   * @return <code>true</code> si le code correspond à une fin de traitement.<br>
   *         <code>false</code> si le code indique que tout s'est bien passé, ou qu'une autre erreur s'est produite.
   */
  public static boolean isEOF(int returnCode) {
    return avutil.AVERROR_EOF == returnCode || avutil.AVERROR_EAGAIN() == returnCode;
  }

  /**
   * Propage une {@link FFmpegException} si le code FFmpeg donné indique une erreur.
   * 
   * @param returnCode
   *          Le code retour FFmpeg.
   * @return Le code retour FFmpeg.
   * @throws FFmpegException
   *           Si le code donné indique une erreur FFmpeg.
   */
  public static int checkAndThrow(int returnCode) throws FFmpegException {
    if (returnCode < 0) {
      throw new FFmpegException(returnCode);
    }
    return returnCode;
  }

  /**
   * Log de détail d'une erreur si le code retour indique une erreur FFmpeg.
   * 
   * @param returnCode
   *          Le code retour FFmpeg.
   * @return Le code retour FFmpeg.
   */
  public static int checkAndLog(int returnCode) {
    final String error = getFFmpegError(returnCode);
    if (error != null) {
      LOG.error(error);
    }
    return returnCode;
  }

  /**
   * Log en debug de détail d'une erreur si le code retour indique une erreur FFmpeg.
   * 
   * @param returnCode
   *          Le code retour FFmpeg.
   * @return Le code retour FFmpeg.
   */
  public static int checkAndLogDebug(int returnCode) {
    final String error = getFFmpegError(returnCode);
    if (error != null) {
      LOG.debug(error);
    }
    return returnCode;
  }

  /**
   * Log en warn de détail d'une erreur si le code retour indique une erreur FFmpeg.
   * 
   * @param returnCode
   *          Le code retour FFmpeg.
   * @return Le code retour FFmpeg.
   */
  public static int checkAndLogWarn(int returnCode) {
    final String error = getFFmpegError(returnCode);
    if (error != null) {
      LOG.info(error);
    }
    return returnCode;
  }

  /**
   * Envoie une exception si l'objet donné n'a pas pu être alloué.
   * 
   * @param tclass
   *          La classe de l'objet à vérifier.
   * @param alloc
   *          L'objet alloué.
   * @return L'objet alloué.
   * @throws FFmpegException
   *           Si l'objet n'a pas été alloué.
   */
  public static <T> T checkAllocation(Class<T> tclass, T alloc) {
    return checkAllocation(tclass.getSimpleName(), alloc);
  }

  /**
   * Envoie une exception si l'objet donné n'a pas pu être alloué.
   * 
   * @param alloc
   *          L'objet alloué.
   * @return L'objet alloué.
   * @throws FFmpegException
   *           Si l'objet n'a pas été alloué.
   */
  public static <T> T checkAllocation(T alloc) {
    return checkAllocation("Impossible d'allouer l'objet", alloc);
  }

  /**
   * Envoie une exception si l'objet donné n'a pas pu être alloué.
   * 
   * @param message
   *          Le message à afficher dans le cas où l'objet est <code>null</code>.
   * @param alloc
   *          L'objet alloué.
   * @return L'objet alloué.
   * @throws FFmpegException
   *           Si l'objet n'a pas été alloué.
   */
  public static <T> T checkAllocation(String message, T alloc) {
    if (alloc == null) {
      throw new FFmpegException(message);
    }
    return alloc;
  }

  /**
   * @param message
   *          Le message de l'erreur.
   * @param cause
   *          La cause de l'erreur.
   */
  public FFmpegException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param message
   *          Le message de l'erreur.
   */
  public FFmpegException(String message) {
    super(message);
  }

  /**
   * @param cause
   *          La cause de l'erreur.
   */
  public FFmpegException(Throwable cause) {
    super(cause);
  }

  /**
   * @param code
   *          Le code d'erreur rencontrée.
   */
  public FFmpegException(int code) {
    super(getFFmpegError(code));
  }
}
