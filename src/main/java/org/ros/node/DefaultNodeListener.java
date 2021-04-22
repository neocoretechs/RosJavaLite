package org.ros.node;

/**
 * A {@link NodeListener} which provides empty defaults for all signals.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class DefaultNodeListener implements NodeListener {

  @Override
  public void onStart(ConnectedNode connectedNode) {}

  @Override
  public void onShutdown(Node node) {}

  @Override
  public void onShutdownComplete(Node node) {}

  @Override
  public void onError(Node node, Throwable throwable) {}
}
