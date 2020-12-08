/*
 * Copyright (C) 2012 Google Inc.
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

package org.ros.master.client;

import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.master.MasterServer;
import org.ros.internal.node.topic.TopicDeclaration;
import org.ros.node.Node;
import org.ros.node.service.ServiceServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * A remote client for obtaining system information from a master.
 * 
 * @author Keith M. Hughes
 */
public class MasterStateClient {

  /**
   * The node doing the calling.
   */
  private final Node caller;

  /**
   * Client for speaking to the master.
   */
  private final MasterClient masterClient;

  public MasterStateClient(Node caller, URI masterUri) throws IOException {
    this.caller = caller;
    masterClient = new MasterClient( new InetSocketAddress(InetAddress.getByName(masterUri.getHost()), 8090), 60000, 60000);
  }
  
  public MasterStateClient(Node caller, InetSocketAddress masterUri) throws IOException {
	this.caller = caller;
	masterClient = new MasterClient(masterUri, 60000, 60000);
  }
  /**
   * @param nodeName
   *          the name of the {@link Node} to lookup
   * @return the {@link InetSocketAddress} of the {@link Node} with the given name
   */
  public InetSocketAddress lookupNode(String nodeName) {
    Response<InetSocketAddress> response = masterClient.lookupNode(caller.getName(), nodeName);
    return response.getResult();
  }

  /**
   * @return the {@link InetSocketAddress0} of the {@link MasterServer}
   */
  public InetSocketAddress getUri() {
    Response<InetSocketAddress> response = masterClient.getUri(caller.getName());
    return response.getResult();
  }

  /**
   * @param serviceName
   *          the name of the {@link ServiceServer} to look up
   * @return the {@link InetSocketAddress} of the {@link ServiceServer} with the given name
   */
  public InetSocketAddress lookupService(String serviceName) {
    Response<InetSocketAddress> result = masterClient.lookupService(caller.getName(), serviceName);
    return result.getResult();
  }

  /**
   * @param subgraph
   *          the subgraph of the topics
   * @return a {@link List} of {@link TopicDeclaration}s for published topics
   */
  public List<TopicDeclaration> getPublishedTopics(String subgraph) {
    // TODO(keith): Figure out what to turn the topic definition into.
    throw new UnsupportedOperationException();
  }

  /**
   * @return a {@link List} of {@link TopicType}s known by the master
   */
  public List<TopicType> getTopicTypes() {
    Response<List<TopicType>> result = masterClient.getTopicTypes(caller.getName());
    return result.getResult();
  }

  /**
   * @return the current {@link SystemState}
   */
  public SystemState getSystemState() {
    Response<SystemState> result = masterClient.getSystemState(caller.getName());
    return result.getResult();
  }
}
