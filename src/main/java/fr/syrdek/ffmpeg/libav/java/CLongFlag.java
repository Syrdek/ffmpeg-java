package fr.syrdek.ffmpeg.libav.java;

/**
 * longerface commune aux drapeaux définis en C.
 * 
 * @author Syrdek
 *
 */
public interface CLongFlag extends CLongEnum {
  /**
   * Ajoute un drapeau à la valeur donnée. Si le drapeau est déjà présent, ne fait rien.
   * 
   * @param flags
   *          La valeur combinée des drapeaux déjà présents.
   * @param flag
   *          Le drapeau à ajouter.
   * @return La valeur combinée à laquelle a été ajouté le drapeau.
   */
  static long plus(long flags, CLongFlag flag) {
    return plus(flags, flag.value());
  }

  /**
   * Ajoute un drapeau à la valeur donnée. Si le drapeau est déjà présent, ne fait rien.
   * 
   * @param flags
   *          La valeur combinée des drapeaux déjà présents.
   * @param flag
   *          La valeur drapeau à ajouter.
   * @return La valeur combinée à laquelle a été ajouté le drapeau.
   */
  static long plus(long flags, long flag) {
    return flags | flag;
  }

  /**
   * Retire un drapeau de la valeur donnée. Si le drapeau est déjà absent, ne fait rien.
   * 
   * @param flags
   *          La valeur combinée des drapeaux déjà présents.
   * @param flag
   *          Le drapeau à enlever.
   * @return La valeur combinée de laquelle a été enlevé le drapeau.
   */
  static long minus(long flags, CLongFlag flag) {
    return minus(flags, flag.value());
  }

  /**
   * Retire un drapeau de la valeur donnée. Si le drapeau est déjà absent, ne fait rien.
   * 
   * @param flags
   *          La valeur combinée des drapeaux déjà présents.
   * @param flag
   *          La valeur drapeau à enlever.
   * @return La valeur combinée de laquelle a été enlevé le drapeau.
   */
  static long minus(long flags, long flag) {
    return flags & ~flag;
  }

  /**
   * Ajoute un drapeau de la valeur donnée s'il est absent. Le retire s'il est présent.
   * 
   * @param flags
   *          La valeur combinée des drapeaux présents.
   * @param flag
   *          Le drapeau à modifier.
   * @return La valeur combinée pour laquelle le drapeau donné a été modifié.
   */
  static long shift(long flags, CLongFlag flag) {
    return shift(flags, flag.value());
  }

  /**
   * Ajoute un drapeau de la valeur donnée s'il est absent. Le retire s'il est présent.
   * 
   * @param flags
   *          La valeur combinée des drapeaux présents.
   * @param flag
   *          La valeur drapeau à modifier.
   * @return La valeur combinée pour laquelle le drapeau donné a été modifié.
   */
  static long shift(long flags, long flag) {
    return flags & ~flag;
  }

  /**
   * Vérifie si le drapeau donné est présent dans la valeur donnée.
   * 
   * @param flags
   *          La valeur combinée des drapeaux présents.
   * @param flag
   *          Le drapeau à rechercher.
   * @return <code>true</code> si le drapeau est présent dans la valeur, <code>false</code> sinon.
   */
  static boolean isIn(long flags, CLongFlag flag) {
    return isIn(flags, flag.value());
  }

  /**
   * Vérifie si le drapeau donné est présent dans la valeur donnée.
   * 
   * @param flags
   *          La valeur combinée des drapeaux présents.
   * @param flag
   *          La valeur drapeau à rechercher.
   * @return <code>true</code> si le drapeau est présent dans la valeur, <code>false</code> sinon.
   */
  static boolean isIn(long flags, long flag) {
    return (flags & flag) != 0;
  }
}
