package fr.syrdek.ffmpeg.libav.java;

import org.bytedeco.ffmpeg.avutil.AVRational;

/**
 * Outils de gestion des timestamps ffmpeg.
 *
 * @author t0087865
 *
 */
public class TimeUtils {

  private static final String NONE = "none";

  /**
   * Classe statique.
   */
  private TimeUtils() {
    super();
  }

  /**
   * Convertit le timestamp en valeur intelligible pour les logs.
   *
   * @param timestamp
   *          Le timestamp.
   * @return La valeur intelligible.
   */
  public static String timestampToString(long timestamp) {
    if (timestamp < 0) {
      return NONE;
    }
    return String.valueOf(timestamp);
  }

  /**
   * Convertit le timestamp en valeur intelligible pour les logs.
   *
   * @param timestamp
   *          Le timestamp.
   * @param timebase
   *          La base de temps dans laquelle est exprimé le timestamp.
   * @return La valeur intelligible, au format HH:MM:SS.mmm si le timestamp est >= 0, ou none sinon.
   */
  public static String timestampToHms(long timestamp, AVRational timebase) {
    if (timestamp < 0) {
      return NONE;
    }

    if (timebase == null || timebase.den() == 0) {
      return NONE;
    }

    // Multiplie le timestamp par le timebase pour avoir le timestamp en secondes.
    return toHms(timestamp * timebase.num() / timebase.den());
  }

  /**
   * Convertit un timebase en texte.
   *
   * @param timebase
   *          Le timebase.
   * @return Le texte correspondant au timebase.
   */
  public static String toString(AVRational timebase) {
    if (timebase == null) {
      return NONE;
    }
    return timebase.num() + "/" + timebase.den();
  }

  /**
   * @param timebase
   *          La timebase à copier.
   * @return La copie de la timebase donnée en paramètre. <code>null</code> si la timebase donnée est
   *         nulle.<b>Attention</b> La timebase donnée doit être fermée après utilisation via {@link AVRational#close()}
   *         afin de libérer la mémoire allouée.
   */
  public static AVRational copy(final AVRational timebase) {
    if (timebase == null) {
      return null;
    }
    final AVRational copy = new AVRational();
    copy.num(timebase.num()).den(timebase.den());
    return copy;
  }

  /**
   * Convertit un nombre de secondes en <code>heures:minutes:secondes.millis</code> pour affichage.
   *
   * @param sec
   *          Le nombre de secondes.
   * @return Le nombre de secondes au format HH:MM:SS.mmm
   */
  public static String toHms(double sec) {
    return new StringBuilder()
        .append(String.format("%02d", (int) (sec / 3600)))
        .append(":")
        .append(String.format("%02d", (int) (sec / 60 % 60)))
        .append(":")
        .append(String.format("%02d", (int) (sec % 60)))
        .append(".")
        .append(String.format("%03d", (int) (sec * 1000)))
        .toString();
  }

}
