package fr.syrdek.ffmpeg.libav.java;

/**
 * Interface commune aux drapeaux définis en C.
 * 
 * @author Syrdek
 *
 */
public interface CFlag extends CEnum {
  /**
   * Ajoute un drapeau à la valeur donnée. Si le drapeau est déjà présent, ne fait rien.
   * 
   * @param flags
   *          La valeur combinée des drapeaux déjà présents.
   * @param flag
   *          Le drapeau à ajouter.
   * @return La valeur combinée à laquelle a été ajouté le drapeau.
   */
  static int plus(int flags, CFlag flag) {
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
  static int plus(int flags, int flag) {
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
  static int minus(int flags, CFlag flag) {
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
  static int minus(int flags, int flag) {
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
  static int shift(int flags, CFlag flag) {
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
  static int shift(int flags, int flag) {
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
  static boolean isIn(int flags, CFlag flag) {
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
  static boolean isIn(int flags, int flag) {
    return (flags & flag) != 0;
  }
}
