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

import java.util.Set;

/**
 * Facility for contacting the remote master and issuing commands via the remote invokable methods.
 * The transport is accomplished via the RemoteRequestinterface implementors.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2020,2021
 */
public class SlaveRpcEndpointImpl implements SlaveRpcEndpoint {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(SlaveRpcEndpointImpl.class);

  //private final SlaveServer slave;
  private RemoteClient remoteSlave;

  public SlaveRpcEndpointImpl(String remoteHost, int remotePort) throws IOException {
    //this.slave = slave;
	  remoteSlave = new RemoteClient(remoteHost, remotePort);
  }

  @Override
  public List<Object> getBusStats(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
				"getBusStats",
				callerId);   
    return (List<Object>) remoteSlave.queue(rri);//slave.getBusStats(callerId);
  }

  @Override
  public List<Object> getBusInfo(String callerId) {
		RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
				"getBusInfo",
				callerId); 
    List<Object> busInfo = (List<Object>) remoteSlave.queue(rri);//slave.getBusInfo(callerId);
    return Response.newSuccess("bus info", busInfo).toList();
  }

  @Override
  public List<Object> getMasterUri(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
				"getMasterUri"
				); 
    InetSocketAddress uri = (InetSocketAddress) remoteSlave.queue(rri);//slave.getMasterUri();
    return new Response<String>(StatusCode.SUCCESS, "", uri.toString()).toList();
  }

  @Override
  public List<Object> shutdown(String callerId, String message) {
    log.info("Shutdown requested by " + callerId + " with message \"" + message + "\"");
    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
			"shutdown"
			); 
	//slave.shutdown();
    remoteSlave.close();
    return Response.newSuccess("Shutdown successful.", remoteSlave.queue(rri)).toList();
  }

  @Override
  public List<Object> getPid(String callerId) {
    try {
        RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
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
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
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
	    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
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
	    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
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
  public List<Object> paramUpdate(String callerId, String key, Object value) {
    return parameterUpdate(key, value);
  }

  @Override
  public List<Object> publisherUpdate(String callerId, String topicName, Object[] publishers) {
    ArrayList<InetSocketAddress> publisherUris = new ArrayList<InetSocketAddress>(publishers.length);
      for (Object publisher : publishers) {
        InetSocketAddress uri = (InetSocketAddress)publisher;
        publisherUris.add(uri);
      }
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
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
    ProtocolDescription protocol = null;
    RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.SlaveServer",
				"requestTopic",
				topic, requestedProtocols); 
    // The remote request may reference a topic with no publishers
    // in that case the call on the remote side returns null, which is translated to
    // an empty object instance by the time it gets here, so check for proper
    // type returned before cast fail
    Object res = remoteSlave.queue(rri);//slave.requestTopic(topic, requestedProtocols);
    List<Object> response = null;
    if( res instanceof ProtocolDescription ) {
    	protocol = (ProtocolDescription)res;
    	response = Response.newSuccess(protocol.toString(), protocol.toList()).toList();
    	if (DEBUG) {
    		log.info("Requested topic " + topic + " with proto:" + requestedProtocols + ". response: "+ response.toString());
    	}
    } else {
    	response = Response.newNotfound("Requested topic:"+topic+" failed to return a valid protocol response from requested protocol:"+requestedProtocols+". Publisher may not exist.", 1).toList();
    }
    return response;
  }

  /**
   * Re-use this slave for connection to something else. Shut down previous remote client
   * and create a new one with the given parameters.
   */
  @Override
  public void setConfig(RpcClientConfigImpl config) {
	remoteSlave.close();
	try {
		remoteSlave = new RemoteClient(config.getSeverURL().getHostName(), config.getSeverURL().getPort());
	} catch (IOException e) {
		log.fatal("Reconnection to remote slave at "+config.getSeverURL().getHostString()+" FAILED due to:"+e.getMessage());
		e.printStackTrace();
	}
  }

  @Override
  public void shutDown() {
	remoteSlave.close();
  }


}
