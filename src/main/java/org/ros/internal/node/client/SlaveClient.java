package org.ros.internal.node.client;

import org.ros.exception.RemoteException;
import org.ros.exception.RemoteNotFoundException;
import org.ros.internal.node.response.IntegerResultFactory;
import org.ros.internal.node.response.ProtocolDescriptionResultFactory;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.response.TopicListResultFactory;
import org.ros.internal.node.response.UriResultFactory;
import org.ros.internal.node.response.VoidResultFactory;
import org.ros.internal.node.rpc.SlaveRpcEndpoint;
import org.ros.internal.node.rpc.SlaveRpcEndpointImpl;
import org.ros.internal.node.topic.TopicDeclaration;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.namespace.GraphName;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.Collection;
import java.util.List;

/**
 * Create a client to the {@link org.ros.internal.node.rpc.RpcEndpoint} via {@link SlaveRpcEndpointImpl} 
 * from the socket address passed in constructor.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2025
 */
public class SlaveClient extends Client<SlaveRpcEndpoint> {

  private final GraphName nodeName;
  
  public SlaveClient(GraphName nodeName, InetSocketAddress subscriberSlaveUri) throws IOException {
	    super(subscriberSlaveUri, 60000, 60000);
	    this.nodeName = nodeName;
	    rpcEndpoint = new SlaveRpcEndpointImpl(subscriberSlaveUri.getHostName(), subscriberSlaveUri.getPort());
  }
  
  public List<Object> getBusStats() {
    throw new UnsupportedOperationException();
  }

  public List<Object> getBusInfo() {
    throw new UnsupportedOperationException();
  }

  public Response<InetSocketAddress> getMasterUri() {
    return Response.fromListChecked(rpcEndpoint.getMasterUri(nodeName.toString()), new UriResultFactory());
  }

  public Response<Void> shutdown(String message) {
    return Response.fromListChecked(rpcEndpoint.shutdown("/master", message), new VoidResultFactory());
  }

  public Response<Integer> getPid() {
    return Response.fromListChecked(rpcEndpoint.getPid(nodeName.toString()), new IntegerResultFactory());
  }

  public Response<List<TopicDeclaration>> getSubscriptions() {
    return Response.fromListChecked(rpcEndpoint.getSubscriptions(nodeName.toString()),
        new TopicListResultFactory());
  }

  public Response<List<TopicDeclaration>> getPublications() {
    return Response.fromListChecked(rpcEndpoint.getPublications(nodeName.toString()),
        new TopicListResultFactory());
  }

  public Response<Void> paramUpdate(GraphName name, Object value) {
    return Response.fromListChecked(rpcEndpoint.paramUpdate(nodeName.toString(), name.toString(), value),
        new VoidResultFactory());
  }

  public Response<Void> publisherUpdate(GraphName topic, List<InetSocketAddress> publisherUris) {
    return Response.fromListChecked(
        rpcEndpoint.publisherUpdate(nodeName.toString(), topic.toString(), publisherUris.toArray()),
        new VoidResultFactory());
  }

  public Response<ProtocolDescription> requestTopic(GraphName topic,
      Collection<String> requestedProtocols) throws RemoteException, RemoteNotFoundException {
    return Response.fromListCheckedNotFound(rpcEndpoint.requestTopic(nodeName.toString(), topic.toString(),
        new Object[][] { requestedProtocols.toArray() }), new ProtocolDescriptionResultFactory());
  }
}
