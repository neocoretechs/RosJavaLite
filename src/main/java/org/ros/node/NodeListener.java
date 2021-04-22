package org.ros.node;

/**
 * A listener for lifecycle events on a {@link Node}. The events include start, shutdown
 * shutodwn complete, and error.<p/>
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public interface NodeListener {

  /**
   * Called when the {@link Node} has started and successfully connected to the
   * master.
   * 
   * @param connectedNode the {@link ConnectedNode} that has been started
   */
  void onStart(ConnectedNode connectedNode);

  /**
   * Called when the {@link ConnectedNode} has started shutting down. Shutdown
   * will be delayed, although not indefinitely, until all {@link NodeListener}s
   * have returned from this method.
   * <p>
   * Since this method can potentially delay {@link ConnectedNode} shutdown, it
   * is preferred to use {@link #onShutdownComplete(Node)} when
   * {@link ConnectedNode} resources are not required during the method call.
   * 
   * @param node the {@link Node} that has started shutting down
   */
  void onShutdown(Node node);

  /**
   * Called when the {@link Node} has shut down.
   * 
   * @param node the {@link Node} that has shut down
   */
  void onShutdownComplete(Node node);

  /**
   * Called when the {@link Node} experiences an unrecoverable error.
   * 
   * @param node the {@link Node} that experienced the error
   * @param throwable the {@link Throwable} describing the error condition
   */
  void onError(Node node, Throwable throwable);
}
