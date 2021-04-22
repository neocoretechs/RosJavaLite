package org.ros.node;

/**
 * A {@link NodeMain} which provides empty defaults for all signals.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public abstract class AbstractNodeMain implements NodeMain {

  @Override
  public void onStart(ConnectedNode connectedNode) { }

  @Override
  public void onShutdown(Node node) { }

  @Override
  public void onShutdownComplete(Node node) { }

  @Override
  public void onError(Node node, Throwable throwable) { }
}
