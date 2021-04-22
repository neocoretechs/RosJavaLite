package org.ros.node;

import java.util.Collection;

/**
 * Builds new {@link Node}s driven by (@link NodeConfiguration} parameters.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public interface NodeFactory {

  /**
   * Build a new {@link Node} with the given {@link NodeConfiguration}.
   * 
   * @param configuration the {@link NodeConfiguration} for the new {@link Node}
   * @return a new {@link Node}
   */
  Node newNode(NodeConfiguration configuration);

  /**
   * Build a new {@link Node} with the given {@link NodeConfiguration} and {@link NodeListener}s.
   * 
   * @param configuration the {@link NodeConfiguration} for the new {@link Node}
   * @param listeners a collection of {@link NodeListener} instances which will be registered with the node (can be {@code null})
   * @return a new {@link Node}
   */
  Node newNode(NodeConfiguration configuration, Collection<NodeListener> listeners);
}
