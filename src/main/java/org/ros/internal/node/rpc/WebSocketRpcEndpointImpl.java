package org.ros.internal.node.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.internal.node.client.RemoteClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.RemoteRequest;
import org.ros.internal.node.server.RemoteRequestInterface;
import org.ros.namespace.GraphName;

public class WebSocketRpcEndpointImpl extends WebSocketServer implements MasterRpcEndpoint, ParameterServerRpcEndpoint {
	  private static final boolean DEBUG = false;
	  private static final Log log = LogFactory.getLog(MasterRpcEndpointImpl.class);
	  private RemoteClient remoteMaster;
	  private RemoteClient remoteParameter;
	  
	  AtomicInteger clients = new AtomicInteger(0);

	  public WebSocketRpcEndpointImpl(ServerSocketChannel chan) {
	    super(chan);
	  }
	  
	  @Override
	  public void shutDown() {
			remoteMaster.close();
			remoteParameter.close();
	  }
	  /**
	   * Set for future use where we may need to connect to a higher master, perhaps for failover,
	   * although in practice the master seems robust. The use case would be loss of the entire
	   * node. remoteMaster and remoteParameter are both closed, shutdown awaited, then restarted at
	   * new config parameters. 
	   */
	  @Override
	  public void setConfig(RpcClientConfigImpl config) {
		remoteMaster.close();
		remoteParameter.close();
		try {
			remoteMaster = new RemoteClient(config.getSeverURL().getHostName(), config.getSeverURL().getPort());
			remoteParameter = new RemoteClient(config.getSeverURL().getHostName(), config.getSeverURL().getPort()+1);
		} catch (IOException e) {
			log.fatal("Reconnection to remote master and parameter servers at "+config.getSeverURL().getHostString()+" FAILED due to:"+e.getMessage());
			e.printStackTrace();
		}

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
	public List<Object> unsubscribeParam(String callerId, String callerApi, String callerPort, String key) {
		  throw new UnsupportedOperationException();
	}

	 @Override
	  public List<Object> getPid(String callerId) {
		RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", "getPid");  
	    return Response.newSuccess("server pid", remoteMaster.queue(rri)).toList();
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
	  public List<Object> registerPublisher(String callerId, String topicName, String topicMessageType,
	      String callerSlaveUri, String port) {
		  RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
				  "registerPublisher",
				  GraphName.of(callerId), new InetSocketAddress(callerSlaveUri, Integer.valueOf(port)), GraphName.of(topicName), topicMessageType);
		  List<InetSocketAddress> subscribers =
	         //master.registerPublisher(GraphName.of(callerId), 
	        //new InetSocketAddress(callerSlaveUri, Integer.valueOf(port)), GraphName.of(topicName), topicMessageType);
	    		(List<InetSocketAddress>)remoteMaster.queue(rri);
	      return Response.newSuccess("Success", subscribers).toList();
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
	  public List<Object> lookupService(String callerId, String serviceName) {
		RemoteRequestInterface rri = new RemoteRequest("org.ros.internal.node.server.master.MasterServer", 
				  "lookupService",
				  GraphName.of(serviceName));
		Object sock = remoteMaster.queue(rri);//master.lookupService(GraphName.of(serviceName)); 
	    if (sock != null && sock instanceof InetSocketAddress) {
	      InetSocketAddress slaveUri = (InetSocketAddress)sock;
	      return Response.newSuccess("Success", slaveUri.toString()).toList();
	    }
	    return Response.newError("No such service.", callerId+" "+serviceName).toList();
	  }

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
	    broadcast(conn + " has left the room!");
	    System.out.println(conn + " has left the room!");
	    if(clients.decrementAndGet() <= 0) {
	      System.out.println("No more clients left, exiting");
	      System.exit(0);
	    }
	}

	 @Override
	  public void onError(WebSocket conn, Exception ex) {
	    ex.printStackTrace();
	  }

	  @Override
	  public void onStart() {
	    System.out.println("Server started!");
	  }


	@Override
	public void onMessage(WebSocket conn, String message) {
	    broadcast(message);
	    System.out.println(conn + ": " + message);
		
	}
	
	 @Override
	  public void onMessage(WebSocket conn, ByteBuffer message) {
	    broadcast(message.array());
	    System.out.println(conn + ": " + message);
	  }
	 
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		   conn.send("Welcome to the server!"); //This method sends a message to the new client
		   broadcast("new connection: " + handshake.getResourceDescriptor()); //This method sends a message to all clients connected
		   if(clients.get() == 0) {
		      broadcast("You are the first client to join");
		   }
		   System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
		   clients.incrementAndGet();
	}


}
