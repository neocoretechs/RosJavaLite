package org.ros.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A group of listeners.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ListenerGroup<T> {

  private final static int DEFAULT_QUEUE_CAPACITY = 128;

  private final ExecutorService executorService;
  private final Collection<EventDispatcher<T>> eventDispatchers;

  public ListenerGroup(ExecutorService executorService) {
    this.executorService = executorService;
    eventDispatchers = new ArrayList<EventDispatcher<T>>();
  }

  /**
   * Adds a listener to the {@link ListenerGroup}.
   * 
   * @param listener the listener to add
   * @param queueCapacity the maximum number of events to buffer
   * @return the {@link EventDispatcher} responsible for calling the specified listener
   */
  public EventDispatcher<T> add(T listener, int queueCapacity) {
    EventDispatcher<T> eventDispatcher = new EventDispatcher<T>(listener, queueCapacity);
    eventDispatchers.add(eventDispatcher);
    executorService.execute(eventDispatcher);
    return eventDispatcher;
  }

  /**
   * Adds the specified listener to the {@link ListenerGroup} with the queue
   * limit set to {@link #DEFAULT_QUEUE_CAPACITY}.
   * 
   * @param listener the listener to add
   * @return the {@link EventDispatcher} responsible for calling the specified listener
   */
  public EventDispatcher<T> add(T listener) {
    return add(listener, DEFAULT_QUEUE_CAPACITY);
  }

  /**
   * Adds all the specified listeners to the {@link ListenerGroup}.
   * 
   * @param listeners the listeners to add
   * @param limit the maximum number of events to buffer
   * @return a {@link Collection} of {@link EventDispatcher}s responsible for calling the specified listeners
   */
  public Collection<EventDispatcher<T>> addAll(Collection<T> listeners, int limit) {
    Collection<EventDispatcher<T>> eventDispatchers = new ArrayList<EventDispatcher<T>>();
    for (T listener : listeners) {
      eventDispatchers.add(add(listener, limit));
    }
    return eventDispatchers;
  }

  /**
   * Adds all the specified listeners to the {@link ListenerGroup} with the
   * queue capacity for each set to {@link Integer#MAX_VALUE}.
   * 
   * @param listeners the listeners to add
   * @return a {@link Collection} of {@link EventDispatcher}s responsible for calling the specified listeners
   */
  public Collection<EventDispatcher<T>> addAll(Collection<T> listeners) {
    return addAll(listeners, DEFAULT_QUEUE_CAPACITY);
  }

  /**
   * @return the number of listeners in the group
   */
  public int size() {
    return eventDispatchers.size();
  }

  /**
   * Signals all listeners.
   * <p>
   * Each {@link SignalRunnable} is executed in a separate thread.
   */
  public void signal(SignalRunnable<T> signalRunnable) {
    for (EventDispatcher<T> eventDispatcher : eventDispatchers) {
      eventDispatcher.signal(signalRunnable);
    }
  }

  /**
   * Signals all listeners and waits for the result.
   * <p>
   * Each {@link SignalRunnable} is executed in a separate thread. In the event
   * that the {@link SignalRunnable} is be dropped from the
   * {@link EventDispatcher}'s queue and thus not executed, this method will
   * block for the entire specified timeout.
   * 
   * @return {@code true} if all listeners completed within the specified time limit, {@code false} otherwise
   * @throws InterruptedException
   */
  public boolean signal(final SignalRunnable<T> signalRunnable, long timeout, TimeUnit unit)
      throws InterruptedException {
    Collection<EventDispatcher<T>> copy = new ArrayList<EventDispatcher<T>>(eventDispatchers);
    final CountDownLatch latch = new CountDownLatch(copy.size());
    for (EventDispatcher<T> eventDispatcher : copy) {
      eventDispatcher.signal(new SignalRunnable<T>() {
        @Override
        public void run(T listener) {
          signalRunnable.run(listener);
          latch.countDown();
        }
      });
    }
    return latch.await(timeout, unit);
  }

  public void shutdown() {
    for (EventDispatcher<T> eventDispatcher : eventDispatchers) {
      eventDispatcher.cancel();
    }
  }
}
