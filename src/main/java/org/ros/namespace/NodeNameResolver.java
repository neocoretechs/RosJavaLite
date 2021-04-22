package org.ros.namespace;

import org.ros.node.Node;

/**
 * Resolver for {@link Node} names. {@link Node} namespace must handle the ~name
 * syntax for private names.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class NodeNameResolver extends NameResolver {

  private final GraphName privateNamespace;

  /**
   * @param nodeName
   *          the name of the {@link Node}
   * @param defaultResolver
   *          the {@link NameResolver} to use if asked to resolve a non-private
   *          name
   */
  public NodeNameResolver(GraphName nodeName, NameResolver defaultResolver) {
    super(defaultResolver.getNamespace(), defaultResolver.getRemappings());
    this.privateNamespace = nodeName;
  }

  /**
   * @param name
   *          name to resolve
   * @return the name resolved relative to the default or private namespace
   */
  @Override
  public GraphName resolve(GraphName name) {
    GraphName graphName = lookUpRemapping(name);
    if (graphName.isPrivate()) {
      return resolve(privateNamespace, graphName.toRelative());
    }
    return super.resolve(name);
  }

  /**
   * @see #resolve(GraphName)
   */
  @Override
  public GraphName resolve(String name) {
    return resolve(GraphName.of(name));
  }
}
