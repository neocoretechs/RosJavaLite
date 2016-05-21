package org.ros.node;

import org.ros.internal.node.DefaultNode;
import org.ros.concurrent.SharedScheduledExecutorService;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Constructs {@link DefaultNode}s.
 * 
 * @author jg
 */
public class DefaultNodeFactory implements NodeFactory {

  private final ScheduledExecutorService scheduledExecutorService;

  public DefaultNodeFactory(ScheduledExecutorService scheduledExecutorService) {
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public Node newNode(NodeConfiguration nodeConfiguration, Collection<NodeListener> listeners) {
    return new DefaultNode(nodeConfiguration, listeners, scheduledExecutorService);
  }

  @Override
  public Node newNode(NodeConfiguration nodeConfiguration) {
    return newNode(nodeConfiguration, null);
  }
}
