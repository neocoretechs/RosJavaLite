
package org.ros.internal.node.parameter;


import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.namespace.GraphName;
import org.ros.node.parameter.ParameterListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Maintains a list of listeners to a parameter tree such that when a value is updated those
 * listeners are notified of the change and new value.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ParameterManager {

  private final ExecutorService executorService;
  private final Map<GraphName, ListenerGroup<ParameterListener>> listeners;

  public ParameterManager(ExecutorService executorService) {
    this.executorService = executorService;
    listeners = new HashMap<GraphName, ListenerGroup<ParameterListener>>();
  }

  public void addListener(GraphName parameterName, ParameterListener listener) {
    synchronized (listeners) {
      if (!listeners.containsKey(parameterName)) {
        listeners.put(parameterName, new ListenerGroup<ParameterListener>(executorService));
      }
      listeners.get(parameterName).add(listener);
    }
  }

  /**
   * @param parameterName
   * @param value
   * @return the number of listeners called with the new value
   */
  public int updateParameter(GraphName parameterName, final Object value) {
    int numberOfListeners = 0;
    synchronized (listeners) {
      if (listeners.containsKey(parameterName)) {
        ListenerGroup<ParameterListener> listenerCollection = listeners.get(parameterName);
        numberOfListeners = listenerCollection.size();
        listenerCollection.signal(new SignalRunnable<ParameterListener>() {
          @Override
          public void run(ParameterListener listener) {
            listener.onNewValue(value);
          }
        });
      }
    }
    return numberOfListeners;
  }
}
