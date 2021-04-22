package org.ros.concurrent;

import java.util.concurrent.ExecutorService;

/**
 * An interruptable loop that can be run by an {@link ExecutorService}.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public abstract class CancellableLoop implements Runnable {

  private final Object mutex;

  /**
   * {@code true} if the code has been run once, {@code false} otherwise.
   */
  private boolean ranOnce = false;

  /**
   * The {@link Thread} the code will be running in.
   */
  private Thread thread;

  public CancellableLoop() {
    mutex = new Object();
  }

  @Override
  public void run() {
    synchronized (mutex) {
      assert(!ranOnce) : "CancellableLoops cannot be restarted.";
      ranOnce = true;
      thread = Thread.currentThread();
    }
    try {
      setup();
      while (!thread.isInterrupted()) {
        loop();
      }
    } catch (InterruptedException e) {
      handleInterruptedException(e);
    } finally {
      thread = null;
    }
  }

  /**
   * The setup block for the loop. This will be called exactly once before
   * the first call to {@link #loop()}.
   */
  protected void setup() {
  }

  /**
   * The body of the loop. This will run continuously until the
   * {@link CancellableLoop} has been interrupted externally or by calling
   * {@link #cancel()}.
   */
  protected abstract void loop() throws InterruptedException;

  /**
   * An {@link InterruptedException} was thrown.
   */
  protected void handleInterruptedException(InterruptedException e) {
  }

  /**
   * Interrupts the loop.
   */
  public void cancel() {
    if (thread != null) {
      thread.interrupt();
    }
  }

  /**
   * @return {@code true} if the loop is running
   */
  public boolean isRunning() {
    return thread != null && !thread.isInterrupted();
  }
}
