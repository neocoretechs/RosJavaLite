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

package org.ros.node;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.DefaultScheduledExecutorService;
import org.ros.namespace.GraphName;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Executes {@link NodeMain}s in separate threads.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class DefaultNodeMainExecutor implements NodeMainExecutor {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(DefaultNodeMainExecutor.class);

  private final NodeFactory nodeFactory;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<GraphName, List<ConnectedNode>> connectedNodes;
  private final Map<Node, List<NodeMain>> nodeMains;

  private class RegistrationListener implements NodeListener {
    @Override
    public void onStart(ConnectedNode connectedNode) {
      registerNode(connectedNode);
    }

    @Override
    public void onShutdown(Node node) {
    }

    @Override
    public void onShutdownComplete(Node node) {
      unregisterNode(node);
    }

    @Override
    public void onError(Node node, Throwable throwable) {
      log.error("Node error.", throwable);
      unregisterNode(node);
    }
  }

  /**
   * @return an instance of {@link DefaultNodeMainExecutor} that uses a
   *         {@link ScheduledExecutorService} that is suitable for both
   *         executing tasks immediately and scheduling tasks to execute in the
   *         future
   */
  public static NodeMainExecutor newDefault() {
    return newDefault(new NioEventLoopGroup().next()/*new DefaultScheduledExecutorService()*/);
  }

  /**
   * @return an instance of {@link DefaultNodeMainExecutor} that uses the
   *         supplied {@link ExecutorService}
   */
  public static NodeMainExecutor newDefault(ScheduledExecutorService executorService) {
    return new DefaultNodeMainExecutor(new DefaultNodeFactory(executorService), executorService);
  }

  /**
   * @param nodeFactory
   *          {@link NodeFactory} to use for node creation.
   * @param scheduledExecutorService
   *          {@link NodeMain}s will be executed using this
   */
  private DefaultNodeMainExecutor(NodeFactory nodeFactory, ScheduledExecutorService scheduledExecutorService) {
    this.nodeFactory = nodeFactory;
    this.scheduledExecutorService = scheduledExecutorService;
    connectedNodes = new ConcurrentHashMap<GraphName, List<ConnectedNode>>();
    nodeMains = new ConcurrentHashMap<Node, List<NodeMain>>();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        DefaultNodeMainExecutor.this.shutdown();
      }
    }));
  }

  @Override
  public ScheduledExecutorService getScheduledExecutorService() {
    return scheduledExecutorService;
  }

  @Override
  public void execute(final NodeMain nodeMain, final NodeConfiguration nodeConfiguration, final Collection<NodeListener> nodeListeners) {
    // NOTE(damonkohler): To avoid a race condition, we have to make our copy
    // of the NodeConfiguration in the current thread.
    final NodeConfiguration nodeConfigurationCopy = NodeConfiguration.copyOf(nodeConfiguration);
    nodeConfigurationCopy.setDefaultNodeName(nodeMain.getDefaultNodeName());
    assert(nodeConfigurationCopy.getNodeName() != null) : "Node name not specified.";
    if (DEBUG) {
      log.info("Starting node: " + nodeConfigurationCopy.getNodeName());
    }
    scheduledExecutorService.execute(new Runnable() {
      @Override
      public void run() {
        Collection<NodeListener> nodeListenersCopy = new ArrayList<NodeListener>();
        nodeListenersCopy.add(new RegistrationListener());
        nodeListenersCopy.add(nodeMain);
        if (nodeListeners != null) {
          nodeListenersCopy.addAll(nodeListeners);
        }
        // The new Node will call onStart().
        Node node = nodeFactory.newNode(nodeConfigurationCopy, nodeListenersCopy);
        List<NodeMain> nml = nodeMains.get(node);
        if( nml == null ) {
        	nml = new ArrayList<NodeMain>();
            nodeMains.put(node, nml);
        }
        assert(!nml.contains(nodeMain));
        nml.add(nodeMain);
      }
    });
  }

  @Override
  public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
    execute(nodeMain, nodeConfiguration, null);
  }
  
  /**
   * shuts down the nodes in nodeMains whose key is passed
   */
  @Override
  public void shutdownNodeMain(NodeMain nodeMain) {
    List<NodeMain> node = nodeMains./*inverse().*/get(nodeMain);
    if (node != null) {
    	for(int i = node.size()-1; i > 0; i--)
    		safelyShutdownNode((Node)node.get(i));
    }
  }
  /**
   * Shuts down all nodes in connectedNodes map. Retrieves each list and iterates through safely shutting down each node
   */
  @Override
  public void shutdown() {
    synchronized (connectedNodes) {
      for (List<ConnectedNode> connectedNodeList : connectedNodes.values()) {
    	  for(ConnectedNode connectedNode : connectedNodeList)
    		  safelyShutdownNode(connectedNode);
      }
    }
  }

  /**
   * Trap and log any exceptions while shutting down the supplied {@link Node}.
   * 
   * @param node
   *          the {@link Node} to shut down
   */
  private void safelyShutdownNode(Node node) {
    boolean success = true;
    try {
      node.shutdown();
    } catch (Exception e) {
      // Ignore spurious errors during shutdown.
      log.error("Exception thrown while shutting down node.", e);
      // We don't expect any more callbacks from a node that throws an exception
      // while shutting down. So, we unregister it immediately.
      unregisterNode(node);
      success = false;
    }
    if (success) {
      log.info("Shutdown successful.");
    }
  }

  /**
   * Register a {@link ConnectedNode} with the {@link NodeMainExecutor}.
   * 
   * @param connectedNode
   *          the {@link ConnectedNode} to register
   */
  private void registerNode(ConnectedNode connectedNode) {
    GraphName nodeName = connectedNode.getName();
    synchronized (connectedNodes) {
      List<ConnectedNode> cnl = connectedNodes.get(nodeName);
      if( cnl == null ) {
    		cnl = new ArrayList<ConnectedNode>();
    		connectedNodes.put(nodeName,  cnl);
      }
      for (ConnectedNode illegalConnectedNode : cnl) {
    	  if( illegalConnectedNode.equals(connectedNode) ) {
    		  System.err.println(String.format(
    				  "Node name collision. Existing node %s (%s) will be shutdown.", nodeName, illegalConnectedNode.getUri()));
    		  illegalConnectedNode.shutdown();
    	  }
      }
      cnl.add(connectedNode);
    }
  }

  /**
   * Unregister a {@link Node} with the {@link NodeMainExecutor}.
   * 
   * @param node
   *          the {@link Node} to unregister
   */
  private void unregisterNode(Node node) {
	  synchronized(connectedNodes) {
		  connectedNodes.get(node.getName()).remove(node);
	  }
    nodeMains.remove(node);
  }
}
