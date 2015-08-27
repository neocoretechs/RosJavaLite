/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.client;

import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.rpc.RpcClientConfigImpl;
import org.ros.internal.node.rpc.RpcEndpoint;
import org.ros.internal.node.server.RpcServer;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Base class for RPC clients (e.g. MasterClient and SlaveClient).
 * 
 * @author jg
 * 
 * @param <T>
 *          the RPC interface this {@link Client} connects to
 */
abstract class Client<T extends RpcEndpoint> {

  private final InetSocketAddress uri;

  protected T rpcEndpoint = null;

  /**
   * @param subscriberSlaveUri
   *          the {@link URI} to connect to
   * @param interfaceClass
   *          the class literal for the XML-RPC interface
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
