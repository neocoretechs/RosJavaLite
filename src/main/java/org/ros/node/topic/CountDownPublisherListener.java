
package org.ros.node.topic;

import org.ros.internal.node.topic.SubscriberIdentifier;

import org.ros.internal.node.CountDownRegistrantListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link PublisherListener} which uses separate {@link CountDownLatch}
 * instances for all signals.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class CountDownPublisherListener<T> extends CountDownRegistrantListener<Publisher<T>>
    implements PublisherListener<T> {

  private final CountDownLatch shutdownLatch;
  private final CountDownLatch newSubscriberLatch;

  public static <T> CountDownPublisherListener<T> newDefault() {
    return newFromCounts(1, 1, 1, 1, 1);
  }

  /**
   * @param masterRegistrationSuccessCount
   *          the number of successful master registrations to wait for
   * @param masterRegistrationFailureCount
   *          the number of failing master registrations to wait for
   * @param masterUnregistrationSuccessCount
   *          the number of successful master unregistrations to wait for
   * @param masterUnregistrationFailureCount
   *          the number of failing master unregistrations to wait for
   * @param newSubscriberCount
   *          the number of new subscribers to wait for
   */
  public static <T> CountDownPublisherListener<T> newFromCounts(int masterRegistrationSuccessCount,
      int masterRegistrationFailureCount, int masterUnregistrationSuccessCount,
      int masterUnregistrationFailureCount, int newSubscriberCount) {
    return new CountDownPublisherListener<T>(new CountDownLatch(masterRegistrationSuccessCount),
        new CountDownLatch(masterRegistrationFailureCount), new CountDownLatch(
            masterUnregistrationSuccessCount),
        new CountDownLatch(masterUnregistrationFailureCount),
        new CountDownLatch(newSubscriberCount));
  }

  private CountDownPublisherListener(CountDownLatch masterRegistrationSuccessLatch,
      CountDownLatch masterRegistrationFailureLatch,
      CountDownLatch masterUnregistrationSuccessLatch,
      CountDownLatch masterUnregistrationFailureLatch, CountDownLatch newSubscriberLatch) {
    super(masterRegistrationSuccessLatch, masterRegistrationFailureLatch,
        masterUnregistrationSuccessLatch, masterUnregistrationFailureLatch);
    this.newSubscriberLatch = newSubscriberLatch;
    shutdownLatch = new CountDownLatch(1);
  }

  @Override
  public void onNewSubscriber(Publisher<T> publisher, SubscriberIdentifier subscriberIdentifier) {
    newSubscriberLatch.countDown();
  }

  @Override
  public void onShutdown(Publisher<T> publisher) {
    shutdownLatch.countDown();
  }

  /**
   * Wait for the requested number of shutdowns.
   * 
   * @throws InterruptedException
   */
  public void awaitNewSubscriber() throws InterruptedException {
    newSubscriberLatch.await();
  }

  /**
   * Wait for the requested number of new subscribers within the given time
   * period.
   * 
   * @param timeout
   *          the maximum time to wait
   * @param unit
   *          the {@link TimeUnit} of the {@code timeout} argument
   * @return {@code true} if the requested number of new subscribers connect
   *         within the time period {@code false} otherwise.
   * @throws InterruptedException
   */
  public boolean awaitNewSubscriber(long timeout, TimeUnit unit) throws InterruptedException {
    return newSubscriberLatch.await(timeout, unit);
  }

  /**
   * Wait for for shutdown.
   * 
   * @throws InterruptedException
   */
  public void awaitShutdown() throws InterruptedException {
    shutdownLatch.await();
  }

  /**
   * Wait for shutdown within the given time period.
   * 
   * @param timeout
   *          the maximum time to wait
   * @param unit
   *          the {@link TimeUnit} of the {@code timeout} argument
   * @return {@code true} if shutdown happened within the time period,
   *         {@code false} otherwise
   * @throws InterruptedException
   */
  public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
    return shutdownLatch.await(timeout, unit);
  }
}
