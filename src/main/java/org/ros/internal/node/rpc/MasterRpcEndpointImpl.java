package org.ros.internal.node.rpc;

import org.ros.internal.node.client.RemoteClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.RemoteRequest;
import org.ros.internal.node.server.RemoteRequestInterface;
import org.ros.namespace.GraphName;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * A combined RPC endpoint for the master and parameter servers.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 * @author jg
 */
public class MasterRpcEndpointImpl implements MasterRpcEndpoint, ParameterServerRpcEndpoint {

  //private final MasterServer master;
  //private final ParameterServer parameterServer;
	private final RemoteClient remoteMaster;
	private final RemoteClient remoteParameter;
	
  public MasterRpcEndpointImpl(String remoteHost, int remotePort) throws IOException {
    //this.master = master;
    //parameterServer = new ParameterServer();
	  remoteMaster = new RemoteClient(remoteHost, remotePort);
	  remoteParameter = new RemoteClient(remoteHost, remotePort+1);
  }

  @Override
  public List<Object> getPid(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", "getPid");  
    return Response.newSuccess("server pid", remoteMaster.queue(rri)).toList();
  }

  @Override
  public List<Object> getPublishedTopics(String callerId, String subgraph) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "getPublishedTopics",
			  GraphName.of(callerId), GraphName.of(subgraph));
    return Response.newSuccess("current topics", remoteMaster.queue(rri)
        //master.getPublishedTopics(GraphName.of(callerId), GraphName.of(subgraph))
        ).toList();
  }

  @Override
  public List<Object> getTopicTypes(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "getTopicTypes",
			  GraphName.of(callerId));
    return Response.newSuccess("topic types", remoteMaster.queue(rri)
    		//master.getTopicTypes(GraphName.of(callerId))
    		).toList();
  }

  @Override
  public List<Object> getSystemState(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "getSystemState"
			  );
    return Response.newSuccess("current system state", remoteMaster.queue(rri)
    		//master.getSystemState()
    		).toList();
  }

  @Override
  public List<Object> getUri(String callerId) {
	 RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "getUri"
			  );
    return Response.newSuccess("Success", remoteMaster.queue(rri)
    		//master.getUri().toString()
    		).toList();
  }

  @Override
  public List<Object> lookupNode(String callerId, String nodeName) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "lookupNode",
			  GraphName.of(nodeName));
    InetSocketAddress nodeSlaveUri = (InetSocketAddress) remoteMaster.queue(rri);//master.lookupNode(GraphName.of(nodeName));
    if (nodeSlaveUri != null) {
      return Response.newSuccess("Success", nodeSlaveUri.toString()).toList();
    } else {
      return Response.newError("No such node", null).toList();
    }
  }

  @Override
  public List<Object> registerPublisher(String callerId, String topicName, String topicMessageType,
      String callerSlaveUri, String port) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "registerPublisher",
			  GraphName.of(callerId), new InetSocketAddress(callerSlaveUri, Integer.valueOf(port)), GraphName.of(topicName), topicMessageType);
    List<InetSocketAddress> subscribers =
         //master.registerPublisher(GraphName.of(callerId), 
        //new InetSocketAddress(callerSlaveUri, Integer.valueOf(port)), GraphName.of(topicName), topicMessageType);
    		(List<InetSocketAddress>)remoteMaster.queue(rri);
      List<String> urls = new ArrayList<String>();
      for (InetSocketAddress uri : subscribers) {
        urls.add(uri.toString());
      }
      return Response.newSuccess("Success", urls).toList();
  }

  @Override
  public List<Object> unregisterPublisher(String callerId, String topicName, String callerSlaveUri, String port) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "unregisterPublisher",
			  GraphName.of(callerId), GraphName.of(topicName));
    boolean result = (boolean) remoteMaster.queue(rri);//master.unregisterPublisher(GraphName.of(callerId), GraphName.of(topicName));
    return Response.newSuccess("Success", result ? 1 : 0).toList();
  }

  @Override
  public List<Object> registerSubscriber(String callerId, String topicName,String topicMessageType, String callerSlaveUri, String port) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "registerSubscriber",
			  GraphName.of(callerId), new InetSocketAddress(callerSlaveUri, Integer.valueOf(port)), GraphName.of(topicName), topicMessageType);
    List<InetSocketAddress> publishers =
        //master.registerSubscriber(GraphName.of(callerId), new InetSocketAddress(callerSlaveUri, Integer.valueOf(port)),
        //    GraphName.of(topicName), topicMessageType);
    		(List<InetSocketAddress>)remoteMaster.queue(rri);
      //List<String> urls = new ArrayList<String>();
      //for (InetSocketAddress uri : publishers) {
      //  urls.add(uri.toString());
      //}
      return Response.newSuccess("Success", publishers).toList();
  }

  @Override
  public List<Object> unregisterSubscriber(String callerId, String topicName, String callerSlaveUri, String port) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "unregisterSubscriber",
			  GraphName.of(callerId), GraphName.of(topicName));
    boolean result = (boolean) remoteMaster.queue(rri);//master.unregisterSubscriber(GraphName.of(callerId), GraphName.of(topicName));
    return Response.newSuccess("Success", result ? 1 : 0).toList();
  }

  @Override
  public List<Object> lookupService(String callerId, String serviceName) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "lookupService",
			  GraphName.of(serviceName));
    InetSocketAddress slaveUri = (InetSocketAddress) remoteMaster.queue(rri);//master.lookupService(GraphName.of(serviceName));
    if (slaveUri != null) {
      return Response.newSuccess("Success", slaveUri.toString()).toList();
    }
    return Response.newError("No such service.", null).toList();
  }

  @Override
  public List<Object> registerService(String callerId, String serviceName, String serviceUri, String serviceport,
      String callerSlaveUri, String callerport) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "registerService",
			  GraphName.of(callerId), new InetSocketAddress(callerSlaveUri, Integer.valueOf(callerport)), 
			  GraphName.of(serviceName), new InetSocketAddress(serviceUri, Integer.valueOf(serviceport)));
    //master.registerService(GraphName.of(callerId), new InetSocketAddress(callerSlaveUri, Integer.valueOf(callerport)), GraphName.of(
    //      serviceName), new InetSocketAddress(serviceUri, Integer.valueOf(serviceport)));
	  
      return Response.newSuccess("Success", remoteMaster.queue(rri)).toList();
  }

  @Override
  public List<Object> unregisterService(String callerId, String serviceName, String serviceUri, String servicePort) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
			  "unregisterService",
			  GraphName.of(callerId), GraphName.of(serviceName), new InetSocketAddress(serviceUri, Integer.valueOf(servicePort)));
    boolean result =
          //master.unregisterService(GraphName.of(callerId), GraphName.of(serviceName), new InetSocketAddress(serviceUri, Integer.valueOf(servicePort)));
    		(boolean) remoteMaster.queue(rri);
      return Response.newSuccess("Success", result ? 1 : 0).toList();
  }

 
  @Override
  public List<Object> setParam(String callerId, String key, Object value) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.ParameterServer", 
			  "set",
			  GraphName.of(key), value);
    //parameterServer.set(GraphName.of(key), value);
    return Response.newSuccess("Success", remoteParameter.queue(rri)).toList();
  }

  @Override
  public List<Object> getParam(String callerId, String key) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.ParameterServer", 
			  "get",
			  GraphName.of(key));
    Object value = remoteParameter.queue(rri);//parameterServer.get(GraphName.of(key));
    if (value == null) {
      return Response.newError("Parameter \"" + key + "\" is not set.", null).toList();
    }
    return Response.newSuccess("Success", value).toList();
  }

  @Override
  public List<Object> searchParam(String callerId, String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Object> subscribeParam(String callerId, String callerSlaveUri, String slavePort, String key) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.ParameterServer", 
			  "subscribe",
			  GraphName.of(key), NodeIdentifier.forNameAndUri(callerId, callerSlaveUri, Integer.valueOf(slavePort)));
    //parameterServer.subscribe(GraphName.of(key),
    //    NodeIdentifier.forNameAndUri(callerId, callerSlaveUri, Integer.valueOf(slavePort)));
    Object value = remoteParameter.queue(rri);//parameterServer.get(GraphName.of(key));
    if (value == null) {
      // Must return an empty map as the value of an unset parameter.
      value = new HashMap<String, Object>();
    }
    return Response.newSuccess("Success", value).toList();
  }

  @Override
  public List<Object> unsubscribeParam(String callerId, String callerSlaveUri, String callerSlavePort, String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Object> deleteParam(String callerId, String key) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.ParameterServer", 
			  "delete",
			  GraphName.of(key)); 
    //parameterServer.delete(GraphName.of(key));
    return Response.newSuccess("Success", remoteParameter.queue(rri)).toList();
  }

  @Override
  public List<Object> hasParam(String callerId, String key) {
	  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.ParameterServer", 
			  "has",
			  GraphName.of(key));
    return Response.newSuccess("Success",
    		remoteParameter.queue(rri)
    		//parameterServer.has(GraphName.of(key))
    		).toList();
  }

  @Override
  public List<Object> getParamNames(String callerId) {
	RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.ParameterServer", 
			  "getNames"
			  );
    //Collection<GraphName> names = (Collection<GraphName>) remoteParameter.queue(rri);//parameterServer.getNames();
    /*
    List<String> stringNames = new ArrayList<String>();
    for (GraphName name : names) {
      stringNames.add(name.toString());
    }
    */
    return Response.newSuccess("Success", remoteParameter.queue(rri)).toList();
  }

@Override
public void setConfig(RpcClientConfigImpl config) {
	// TODO Auto-generated method stub	
}

}
