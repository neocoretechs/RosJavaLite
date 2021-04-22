package org.ros.node.service;

import org.ros.internal.node.CountDownRegistrantListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ServiceServerListener} which uses {@link CountDownLatch} to track
 * message invocations.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class CountDownServiceServerListener<T, S> extends
    CountDownRegistrantListener<ServiceServer<T, S>> implements ServiceServerListener<T, S> {

  private final CountDownLatch shutdownLatch;

  /**
   * Construct a {@link CountDownServiceServerListener} with all counts set to
   * 1.
   */
  public static <T, S> CountDownServiceServerListener<T, S> newDefault() {
    return newFromCounts(1, 1, 1, 1);
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
   */
  public static <T, S> CountDownServiceServerListener<T, S> newFromCounts(
      int masterRegistrationSuccessCount, int masterRegistrationFailureCount,
      int masterUnregistrationSuccessCount, int masterUnregistrationFailureCount) {
    return new CountDownServiceServerListener<T, S>(new CountDownLatch(
        masterRegistrationSuccessCount), new CountDownLatch(masterRegistrationFailureCount),
        new CountDownLatch(masterUnregistrationSuccessCount), new CountDownLatch(
            masterUnregistrationFailureCount));
  }

  private CountDownServiceServerListener(CountDownLatch masterRegistrationSuccessLatch,
      CountDownLatch masterRegistrationFailureLatch,
      CountDownLatch masterUnregistrationSuccessLatch,
      CountDownLatch masterUnregistrationFailureLatch) {
    super(masterRegistrationSuccessLatch, masterRegistrationFailureLatch,
        masterUnregistrationSuccessLatch, masterUnregistrationFailureLatch);
    shutdownLatch = new CountDownLatch(1);
  }

  @Override
  public void onShutdown(ServiceServer<T, S> server) {
    shutdownLatch.countDown();
  }

  /**
   * Wait for shutdown.
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
   *          the time unit of the {@code timeout} argument
   * @return {@code true} if shutdown happened within the time period,
   *         {@code false} otherwise
   * @throws InterruptedException
   */
  public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
    return shutdownLatch.await(timeout, unit);
  }
}
