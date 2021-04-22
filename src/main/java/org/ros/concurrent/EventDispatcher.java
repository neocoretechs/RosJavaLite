package org.ros.concurrent;


/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <T> the listener type
 */
public class EventDispatcher<T> extends CancellableLoop {
  private final T listener;
  private final CircularBlockingDeque<SignalRunnable<T>> events;

  public EventDispatcher(T listener, int queueCapacity) {
    this.listener = listener;
    events = new CircularBlockingDeque<SignalRunnable<T>>(queueCapacity);
  }

  public void signal(final SignalRunnable<T> signalRunnable) {
    events.addLast(signalRunnable);
    //System.out.println("Event length addLast="+events.length()+" ***"+Thread.currentThread().getName());
  }

  @Override
  public void loop() throws InterruptedException {
    SignalRunnable<T> signalRunnable = events.takeFirst();
    //System.out.println("Event length loop preRun="+events.length()+" ***"+Thread.currentThread().getName());
    signalRunnable.run(listener);
    //System.out.println("Event length loop postRun="+events.length()+" ***"+Thread.currentThread().getName());
  }
}