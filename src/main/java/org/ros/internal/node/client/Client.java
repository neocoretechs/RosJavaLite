package org.ros.internal.node.client;

import org.ros.internal.node.rpc.RpcClientConfigImpl;
import org.ros.internal.node.rpc.RpcEndpoint;
import org.ros.internal.node.server.RpcServer;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Base class for RPC clients (e.g. MasterClient and SlaveClient).
 * @param <T>
 *          the RPC interface this {@link Client} connects to
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
abstract class Client<T extends RpcEndpoint> {

  private final InetSocketAddress uri;

  protected T rpcEndpoint = null;

  /**
   * @param subscriberSlaveUri the {@link URI} to connect to
   * @param connTimeout connection timeout
   * @param replyTimeout reply timeout
   */
  public Client(InetSocketAddress subscriberSlaveUri, int connTimeout, int replyTimeout) {
    this.uri = subscriberSlaveUri;
    RpcClientConfigImpl config = new RpcClientConfigImpl();
    config.setServerURL(subscriberSlaveUri);
    config.setConnectionTimeout(connTimeout);
    config.setReplyTimeout(replyTimeout);  
  }

  /**
   * @return the {@link URI} of the remote {@link RpcServer}
   */
  public InetSocketAddress getRemoteUri() {
    return uri;
  }
}
