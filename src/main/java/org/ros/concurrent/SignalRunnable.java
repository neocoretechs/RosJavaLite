package org.ros.concurrent;

/**
 * Decouples specific listener interfaces from the signaling implementation.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <T>
 *          the type of listener
 */
public interface SignalRunnable<T> {
  /**
   * This method will be called when the listener should be signaled. Users
   * should override this method to call the appropriate listener method(s).
   * 
   * @param listener the listener to signal
   */
  void run(T listener);
}