package org.ros.node;

import org.ros.internal.node.DefaultNode;
import org.ros.concurrent.SharedScheduledExecutorService;

import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Constructs {@link DefaultNode}s.
 * 
 * @author jg
 */
public class DefaultNodeFactory implements NodeFactory {

  private final ScheduledExecutorService scheduledExecutorService;
  private final NioEventLoop eventLoop;

  public DefaultNodeFactory(ScheduledExecutorService scheduledExecutorService) {
	this.eventLoop = (NioEventLoop) scheduledExecutorService;
    this.scheduledExecutorService = eventLoop.next();//new SharedScheduledExecutorService(scheduledExecutorService);
  }

  @Override
  public Node newNode(NodeConfiguration nodeConfiguration, Collection<NodeListener> listeners) {
    return new DefaultNode(nodeConfiguration, listeners, eventLoop);
  }

  @Override
  public Node newNode(NodeConfiguration nodeConfiguration) {
    return newNode(nodeConfiguration, null);
  }
}
