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


import org.ros.internal.node.response.BooleanResultFactory;
import org.ros.internal.node.response.GraphNameListResultFactory;
import org.ros.internal.node.response.IntegerResultFactory;
import org.ros.internal.node.response.ObjectResultFactory;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.response.ResultFactory;
import org.ros.internal.node.response.StringListResultFactory;
import org.ros.internal.node.response.StringResultFactory;
import org.ros.internal.node.response.VoidResultFactory;
import org.ros.internal.node.rpc.MasterRpcEndpointImpl;
import org.ros.internal.node.rpc.ParameterServerRpcEndpoint;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.ParameterServer;
import org.ros.namespace.GraphName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provide access to the RPC API for a ROS {@link ParameterServer}.
 * 
 * @author kwc@willowgarage.com (Ken Conley)
 * @author damonkohler@google.com (Damon Kohler)
 * @author jg
 */
public class ParameterClient extends Client<ParameterServerRpcEndpoint> {
  private static final boolean DEBUG = true;
  private final NodeIdentifier nodeIdentifier;
  private final String nodeName;

  /**
   * Create a new {@link ParameterClient} connected to the specified
   * {@link ParameterServer} Address.
   * @param nodeIdentifier The identifier of our currently executing node
   * 
   * @param uri
   *          the {@link address} of the {@link ParameterServer} to connect to
 * @throws IOException 
 * @throws UnknownHostException 
   */

  public ParameterClient(NodeIdentifier nodeIdentifier, InetSocketAddress uri) throws IOException {
	super(uri, 60000, 60000);
    this.nodeIdentifier = nodeIdentifier;
    nodeName = nodeIdentifier.getName().toString();
    rpcEndpoint = new MasterRpcEndpointImpl(uri.getHostName(), uri.getPort());
}

public Response<Object> getParam(GraphName parameterName) {
    return Response.fromListCheckedFailure(rpcEndpoint.getParam(nodeName, parameterName.toString()),
        new ObjectResultFactory());
  }

  public Response<Void> setParam(GraphName parameterName, Object parameterValue) {
    return Response.fromListChecked(
        rpcEndpoint.setParam(nodeName, parameterName.toString(), parameterValue), new VoidResultFactory());
  }


  public Response<GraphName> searchParam(GraphName parameterName) {
    Response<String> response =
        Response.fromListCheckedFailure(rpcEndpoint.searchParam(nodeName, parameterName.toString()),
            new StringResultFactory());
    return new Response<GraphName>(response.getStatusCode(), response.getStatusMessage(),
        GraphName.of((String) response.getResult()));
  }

  public Response<Object> subscribeParam(GraphName parameterName) {
    return Response.fromListChecked(rpcEndpoint.subscribeParam(nodeName, nodeIdentifier.getUri().getHostName(),
    		String.valueOf(nodeIdentifier.getUri().getPort()), parameterName.toString()), new ObjectResultFactory());
  }

  public Response<Integer> unsubscribeParam(GraphName parameterName) {
    return Response.fromListChecked(
        rpcEndpoint.unsubscribeParam(nodeName, nodeIdentifier.getUri().getHostName(),String.valueOf(nodeIdentifier.getUri().getPort()),
            parameterName.toString()), new IntegerResultFactory());
  }

  public Response<Boolean> hasParam(GraphName parameterName) {
	  if( DEBUG )
		  System.out.println("ParameterClient.hasParam name:"+parameterName+" node "+nodeName+" for RPC endpoint:"+rpcEndpoint);
    return Response.fromListChecked(rpcEndpoint.hasParam(nodeName, parameterName.toString()),
        new BooleanResultFactory());
  }

  public Response<Void> deleteParam(GraphName parameterName) {
    return Response.fromListChecked(rpcEndpoint.deleteParam(nodeName, parameterName.toString()),
        new VoidResultFactory());
  }

  public Response<List<GraphName>> getParamNames() {
    Response<List<GraphName>> response =
        Response.fromListChecked(rpcEndpoint.getParamNames(nodeName), new GraphNameListResultFactory());
    return response;
  }


}
