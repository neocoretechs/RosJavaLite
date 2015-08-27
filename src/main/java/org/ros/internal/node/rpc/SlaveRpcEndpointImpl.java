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

package org.ros.internal.node.rpc;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.node.client.RemoteClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.response.StatusCode;
import org.ros.internal.node.server.RemoteRequest;
import org.ros.internal.node.server.RemoteRequestInterface;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.node.topic.DefaultSubscriber;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.namespace.GraphName;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class SlaveRpcEndpointImpl implements SlaveRpcEndpoint {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(SlaveRpcEndpointImpl.class);

  //private final SlaveServer slave;
  private final RemoteClient remoteSlave;

  public SlaveRpcEndpointImpl(String remoteHost, int remotePort) throws IOException {
    //this.slave = slave;
	  remoteSlave = new RemoteClient(remoteHost, remotePort);
  }

  @Override
  public List<Object> getBusStats(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"getBusStats",
				callerId);   
    return (List<Object>) remoteSlave.queue(rri);//slave.getBusStats(callerId);
  }

  @Override
  public List<Object> getBusInfo(String callerId) {
		RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"getBusInfo",
				callerId); 
    List<Object> busInfo = (List<Object>) remoteSlave.queue(rri);//slave.getBusInfo(callerId);
    return Response.newSuccess("bus info", busInfo).toList();
  }

  @Override
  public List<Object> getMasterUri(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"getMasterUri"
				); 
    InetSocketAddress uri = (InetSocketAddress) remoteSlave.queue(rri);//slave.getMasterUri();
    return new Response<String>(StatusCode.SUCCESS, "", uri.toString()).toList();
  }

  @Override
  public List<Object> shutdown(String callerId, String message) {
    log.info("Shutdown requested by " + callerId + " with message \"" + message + "\"");
    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
			"shutdown"
			); 
	//slave.shutdown();
    return Response.newSuccess("Shutdown successful.", remoteSlave.queue(rri)).toList();
  }

  @Override
  public List<Object> getPid(String callerId) {
    try {
        RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
    			"getPid"
    			); 
      int pid = (int) remoteSlave.queue(rri);//slave.getPid();
      return Response.newSuccess("PID: " + pid, pid).toList();
    } catch (UnsupportedOperationException e) {
      return Response.newFailure("Cannot retrieve PID on this platform.", -1).toList();
    }
  }

  @Override
  public List<Object> getSubscriptions(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"getSubscriptions"
				); 
    Collection<DefaultSubscriber<?>> subscribers = (Collection<DefaultSubscriber<?>>) remoteSlave.queue(rri);//slave.getSubscriptions();
    List<List<String>> subscriptions = new ArrayList<List<String>>();
    for (DefaultSubscriber<?> subscriber : subscribers) {
      subscriptions.add(subscriber.getTopicDeclarationAsList());
    }
    return Response.newSuccess("Success", subscriptions).toList();
  }

  @Override
  public List<Object> getPublications(String callerId) {
	    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"getPublications"
				); 
    Collection<DefaultPublisher<?>> publishers = (Collection<DefaultPublisher<?>>) remoteSlave.queue(rri);//slave.getPublications();
    List<List<String>> publications = new ArrayList<List<String>>();
    for (DefaultPublisher<?> publisher : publishers) {
      publications.add(publisher.getTopicDeclarationAsList());
    }
    return Response.newSuccess("Success", publications).toList();
  }

  private List<Object> parameterUpdate(String parameterName, Object parameterValue) {
	    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"paramUpdate",
				GraphName.of(parameterName), parameterValue); 
    if ((int)remoteSlave.queue(rri) > 0) {
    		//slave.paramUpdate(GraphName.of(parameterName), parameterValue) 
      return Response.newSuccess("Success", null).toList();
    }
    return Response
        .newError("No subscribers for parameter key \"" + parameterName + "\".", null).toList();
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, boolean value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, char value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, byte value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, short value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, int value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, double value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, String value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, List<?> value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, Vector<?> value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> paramUpdate(String callerId, String key, Map<?, ?> value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> publisherUpdate(String callerId, String topicName, Object[] publishers) {
    ArrayList<InetSocketAddress> publisherUris = new ArrayList<InetSocketAddress>(publishers.length);
      for (Object publisher : publishers) {
        InetSocketAddress uri = (InetSocketAddress)publisher;
        publisherUris.add(uri);
      }
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"publisherUpdate",
				callerId, topicName, publisherUris); 
      //slave.publisherUpdate(callerId, topicName, publisherUris);
	 
      return Response.newSuccess("Publisher update received.",  remoteSlave.queue(rri)).toList();
  }

  @Override
  public List<Object> requestTopic(String callerId, String topic, Object[] protocols) {
    Set<String> requestedProtocols = new HashSet<String>();
    for (int i = 0; i < protocols.length; i++) {
      requestedProtocols.add((String) ((Object[]) protocols[i])[0]);
    }
    ProtocolDescription protocol;
    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.SlaverServer",
				"requestTopic",
				topic, requestedProtocols); 
    protocol = (ProtocolDescription) remoteSlave.queue(rri);//slave.requestTopic(topic, requestedProtocols);
    List<Object> response = Response.newSuccess(protocol.toString(), protocol.toList()).toList();
    if (DEBUG) {
      log.info("requestTopic(" + topic + ", " + requestedProtocols + ") response: "
          + response.toString());
    }
    return response;
  }

@Override
public void setConfig(RpcClientConfigImpl config) {
	// TODO Auto-generated method stub	
}


}
