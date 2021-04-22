package org.ros.node;

import org.apache.commons.logging.Log;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.namespace.NodeNameResolver;


import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A node in the ROS graph.
 * -----------------------<p/>
 * An instance of DefaultNode creates ParameterServer, SlaveServer, the Publishers,etc. <p/>
 * 
 * DefaultNodeFactory creates new DefaultNode config listeners executor <p/>
 * 
 * DefaultNodeMainExecutor new Default(ScheduledExecutorService) : create new DefaultNodeFactory<p/>
 * ----------------------<p/>
 * AsynchTCPServer abstract startServer stopServer : constructs ServerSocket <p/>
 * 
 * final AsynchBaseServer extends AsynchTCPServer takes TcpRosServer: does serversocket.accept in run<br/>
 * ----------------------<p/>
 * TcpRosServer creates AsynchBaseServer, ChannelHandlerContext and AsynchTcpWorker with context <p/>
 * 
 * MasterServer and SlaveServer extend RpcServer and create TcpRosServer<p/>
 * ----------------------<p/>
 * abstract RpcServer takes bind and advertise address and creates BaseServer <p/>
 * ----------------------<p/>
 * 
 * final BaseServer takes RpcServer, ServerSocket.accept() then creates TCPWorker <p/>
 * ----------------------<p/>
 * 
 * TCPWorker takes socket and RpcServer and creates the thread which reads ObjectInputStream from 
 * the socket created from ServerSocket.accept in BaseServer.<p/>
 * ----------------------<p/>
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public interface Node {

  /**
   * @return the fully resolved name of this {@link Node}, e.g. "/foo/bar/boop"
   */
  GraphName getName();

  /**
   * Resolve the given name, using ROS conventions, into a full ROS namespace
   * name. Will be relative to the current namespace unless the name is global.
   * 
   * @param name the name to resolve
   * @return fully resolved ros namespace name
   */
  GraphName resolveName(GraphName name);

  /**
   * @see #resolveName(GraphName)
   */
  GraphName resolveName(String name);

  /**
   * @return {@link NodeNameResolver} for this namespace
   */
  NodeNameResolver getResolver();

  /**
   * @return the {@link URI} of this {@link Node}
   */
  InetSocketAddress getUri();

  /**
   * @return {@link URI} of {@link MasterRpcEndpoint} that this node is attached to.
   */
  InetSocketAddress getMasterUri();

  /**
   * @return Logger for this node, which will also perform logging to /rosout.
   */
  Log getLog();


  /**
   * @return the {@link MessageFactory} used by this node
   */
  MessageFactory getTopicMessageFactory();

  /**
   * @return the {@link MessageFactory} used by this node for service responses
   */
  MessageFactory getServiceResponseMessageFactory();

  /**
   * @return the {@link MessageFactory} used by this node for service requests
   */
  MessageFactory getServiceRequestMessageFactory();

  /**
   * Add a new {@link NodeListener} to the {@link Node}.
   * 
   * @param listener the {@link NodeListener} to add
   */
  void addListener(NodeListener listener);

  /**
   * @return the {@link ScheduledExecutorService} that this {@link Node} uses
   */
  ScheduledExecutorService getScheduledExecutorService();

  /**
   * Executes a {@link CancellableLoop} using the {@link Node}'s
   * {@link ScheduledExecutorService}. The {@link CancellableLoop} will be
   * canceled when the {@link Node} starts shutting down.
   * 
   * <p>
   * Any blocking calls executed in the provided {@link CancellableLoop} can
   * potentially delay {@link Node} shutdown and should be avoided.
   * 
   * @param cancellableLoop the {@link CancellableLoop} to execute
   */
  void executeCancellableLoop(CancellableLoop cancellableLoop);

  /**
   * Shut the node down.
   */
  void shutdown();
}
