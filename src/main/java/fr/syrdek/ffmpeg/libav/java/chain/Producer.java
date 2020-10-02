/**
 *
 */
package fr.syrdek.ffmpeg.libav.java.chain;

import java.util.function.Consumer;

/**
 * Produit des éléments adressés à un {@link Consumer}.
 *
 * @author t0087865
 */
public interface Producer<T> {
  /**
   * @return Le consommateur notifié à chaque élément produit.
   */
  Consumer<T> getConsumer();

  /**
   * Définit le consommateur a notifier lorsqu'un élément est produit. Tout consommateur auparavant présent est effacé
   * lors de l'appel à cette méthode.
   *
   * @param consumer
   *          Un consumer qui sera notifié chaque fois qu'un élément est produit.
   */
  void setConsumer(final Consumer<T> consumer);

  /**
   * Ajoute un consommateur a notifier lorsqu'un élément est produit.
   *
   * @param consumer
   *          Un consumer qui sera notifié chaque fois qu'un élément est produit.
   */
  default Producer<T> sendTo(final Consumer<T> consumer) {
    final Consumer<T> c = getConsumer();
    if (c == null) {
      setConsumer(consumer);
    } else {
      setConsumer(c.andThen(consumer));
    }
    return this;
  }
}
