/**
 *
 */
package fr.syrdek.ffmpeg.tests.javacpp.impl;

import java.util.function.Consumer;

/**
 * Implémentation basique d'un {@link Producer}.
 *
 * @author t0087865
 */
public class BaseProducer<T> implements Producer<T> {

  protected Consumer<T> consumer;

  /**
   * Envoie un élément aux consommateurs.
   *
   * @param t
   *          L'élément à envoyer.
   */
  protected void publish(final T t) {
    if (consumer != null) {
      consumer.accept(t);
    }
  }

  @Override
  public Consumer<T> getConsumer() {
    return consumer;
  }

  @Override
  public void setConsumer(Consumer<T> consumer) {
    this.consumer = consumer;
  }
}
