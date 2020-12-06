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
import org.ros.internal.node.response.InetAddressListResultFactory;

import org.ros.internal.node.response.InetSocketAddressListResultFactory;
import org.ros.internal.node.response.InetSocketAddressResultFactory;
import org.ros.internal.node.response.IntegerResultFactory;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.response.SystemStateResultFactory;
import org.ros.internal.node.response.TopicListResultFactory;
import org.ros.internal.node.response.TopicTypeListResultFactory;

import org.ros.internal.node.response.UriResultFactory;
import org.ros.internal.node.response.VoidResultFactory;
import org.ros.internal.node.rpc.MasterRpcEndpoint;
import org.ros.internal.node.rpc.MasterRpcEndpointImpl;

import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.node.server.master.MasterServer;
import org.ros.internal.node.topic.PublisherDeclaration;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.internal.node.topic.TopicDeclaration;
import org.ros.master.client.SystemState;
import org.ros.master.client.TopicSystemState;
import org.ros.master.client.TopicType;
import org.ros.namespace.GraphName;
import org.ros.node.service.ServiceServer;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import java.util.List;

/**
 * Provides access to the RPC API exposed by a {@link MasterServer}.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MasterClient extends Client<MasterRpcEndpoint> {

  /**
   * Create a new {@link MasterClient} connected to the specified
   * {@link MasterServer} URI.
   * 
   * @param uri
   *          the {@link URI} of the {@link MasterServer} to connect to
 * @throws IOException 
   */
  
  public MasterClient(InetSocketAddress uri, int connTimeout, int replyTimeout) throws IOException {
	    super(uri, connTimeout, replyTimeout);
	    rpcEndpoint = new MasterRpcEndpointImpl(uri.getHostName(), uri.getPort());
  }
  /**
   * Registers the given {@link ServiceServer}.
   * 
   * @param slave
   *          the {@link NodeIdentifier} where the {@link ServiceServer} is
   *          running
   * @param service
   *          the {@link ServiceServer} to register
   * @return a void {@link Response}
   */
  public Response<Void> registerService(NodeIdentifier slave, ServiceServer<?, ?> service) {
    return Response.fromListChecked(
    		rpcEndpoint.registerService(slave.getName().toString(),
        service.getName().toString(), 
        service.getUri().getHostName().toString(), String.valueOf(service.getUri().getPort()), 
        slave.getUri().getHostName().toString(), String.valueOf(slave.getUri().getPort())),
        new VoidResultFactory());
  }

  /**
   * Unregisters the specified {@link ServiceServer}.
   * 
   * @param slave
   *          the {@link NodeIdentifier} where the {@link ServiceServer} is
   *          running
   * @param service
   *          the {@link ServiceServer} to unregister
   * @return the number of unregistered services
   */
  public Response<Integer> unregisterService(NodeIdentifier slave, ServiceServer<?, ?> service) {
    return Response.fromListChecked(rpcEndpoint.unregisterService(
        slave.getName().toString(), service.getName().toString(), service.getUri().getHostName(), String.valueOf(service.getUri().getPort())),
        new IntegerResultFactory());
  }

  /**
   * Registers the given {@link Subscriber}. In addition to receiving a list of
   * current {@link Publisher}s, the {@link Subscriber}s {@link SlaveServer}
   * will also receive notifications of new {@link Publisher}s via the
   * publisherUpdate API.
   * 
   * @param slave
   *          the {@link NodeIdentifier} that the {@link Subscriber} is running
   *          on
   * @param subscriber
   *          the {@link Subscriber} to register
   * @return a {@link List} or {@link SlaveServer} XML-RPC API URIs for nodes
   *         currently publishing the specified topic
   */
  public Response<List<InetAddress>> registerSubscriber(NodeIdentifier slave, Subscriber<?> subscriber) {
    return Response.fromListChecked(
    		rpcEndpoint.registerSubscriber(slave.getName().toString(), 
    		 subscriber.getTopicName().toString(), 
    		 subscriber.getTopicMessageType(), 
    		 slave.getUri().getHostName(), String.valueOf(slave.getUri().getPort())), 
    	    new InetAddressListResultFactory());
  }

  /**
   * Unregisters the specified {@link Subscriber}.
   * 
   * @param slave
   *          the {@link NodeIdentifier} where the subscriber is running
   * @param subscriber
   *          the {@link Subscriber} to unregister
   * @return the number of unregistered {@link Subscriber}s
   */
  public Response<Integer> unregisterSubscriber(NodeIdentifier slave, Subscriber<?> subscriber) {
    return Response.fromListChecked(rpcEndpoint.unregisterSubscriber(slave.getName()
        .toString(), subscriber.getTopicName().toString(), slave.getUri().getHostName(), String.valueOf(slave.getUri().getPort())),
        new IntegerResultFactory());
  }

  /**
   * Registers the specified {@link PublisherDeclaration}.
   * 
   * @param publisherDeclaration
   *          the {@link PublisherDeclaration} of the {@link Publisher} to
   *          register
   * @return a {@link List} of the current {@link SlaveServer} URIs which have
   *         {@link Subscriber}s for the published {@link TopicSystemState}
   */
  public Response<List<InetSocketAddress>> registerPublisher(PublisherDeclaration publisherDeclaration) {
    String slaveName = publisherDeclaration.getSlaveName().toString();
    String slaveUri = publisherDeclaration.getSlaveUri().getHostName();
    String slavePort = String.valueOf(publisherDeclaration.getSlaveUri().getPort());
    String topicName = publisherDeclaration.getTopicName().toString();
    String messageType = publisherDeclaration.getTopicMessageType();
    return Response.fromListChecked(
        rpcEndpoint.registerPublisher(slaveName, topicName, messageType, slaveUri, slavePort),
        new InetSocketAddressListResultFactory());
  }

  /**
   * Unregisters the specified {@link PublisherDeclaration}.
   * 
   * @param publisherIdentifier
   *          the {@link PublisherIdentifier} of the {@link Publisher} to
   *          unregister
   * @return the number of unregistered {@link Publisher}s
   */
  public Response<Integer> unregisterPublisher(PublisherIdentifier publisherIdentifier) {
    String slaveName = publisherIdentifier.getNodeName().toString();
    String slaveUri = publisherIdentifier.getNodeUri().getHostName();
    String slavePort = String.valueOf(publisherIdentifier.getNodeUri().getPort());
    String topicName = publisherIdentifier.getTopicName().toString();
    return Response.fromListChecked(
        rpcEndpoint.unregisterPublisher(slaveName, topicName, slaveUri, slavePort),
        new IntegerResultFactory());
  }

  /**
   * @param slaveName
   *          the {@link GraphName} of the caller
   * @param nodeName
   *          the name of the {@link SlaveServer} to lookup
   * @return the {@link URI} of the {@link SlaveServer} with the given name
   */
  public Response<URI> lookupNode(GraphName slaveName, String nodeName) {
    return Response.fromListChecked(rpcEndpoint.lookupNode(slaveName.toString(), nodeName),
        new UriResultFactory());
  }

  /**
   * @param slaveName
   *          the {@link NodeIdentifier} of the caller
   * @return the {@link URI} of the {@link MasterServer}
   */
  public Response<InetSocketAddress> getUri(GraphName slaveName) {
    return Response.fromListChecked(rpcEndpoint.getUri(slaveName.toString()),
        new InetSocketAddressResultFactory());
  }

  /**
   * @param callerName
   *          the {@link GraphName} of the caller
   * @param serviceName
   *          the name of the {@link ServiceServer} to look up
   * @return the {@link URI} of the {@link ServiceServer} with the given name.
   *         {@link ServiceServer} as a result
   */
  public Response<InetSocketAddress> lookupService(GraphName callerName, String serviceName) {
    return Response.fromListCheckedFailure(
        rpcEndpoint.lookupService(callerName.toString(), serviceName), new InetSocketAddressResultFactory());
  }

  /**
   * @param callerName
   *          the {@link GraphName} of the caller
   * @param subgraph
   *          the subgraph of the topics
   * @return the list of published {@link TopicDeclaration}s
   */
  public Response<List<TopicDeclaration>> getPublishedTopics(GraphName callerName, String subgraph) {
    return Response.fromListChecked(
        rpcEndpoint.getPublishedTopics(callerName.toString(), subgraph),
        new TopicListResultFactory());
  }

  /**
   * Get a {@link List} of all {@link TopicSystemState} message types.
   * 
   * @param callerName
   *          the {@link GraphName} of the caller
   * @return a {@link List} of {@link TopicType}s
   */
  public Response<List<TopicType>> getTopicTypes(GraphName callerName) {
    return Response.fromListChecked(rpcEndpoint.getTopicTypes(callerName.toString()),
        new TopicTypeListResultFactory());
  }

  /**
   * @param callerName
   *          the {@link GraphName} of the caller
   * @return the current {@link SystemState}
   */
  public Response<SystemState> getSystemState(GraphName callerName) {
    return Response.fromListChecked(rpcEndpoint.getSystemState(callerName.toString()),
        new SystemStateResultFactory());
  }
}
