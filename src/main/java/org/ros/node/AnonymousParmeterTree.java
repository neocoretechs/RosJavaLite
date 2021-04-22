package org.ros.node;

import org.ros.internal.node.parameter.DefaultParameterTree;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.parameter.ParameterListener;
import org.ros.node.parameter.ParameterTree;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * Provides generic operations on unnamed ParameterTree.<p/>
 * The ParameterServer is based at a socket address 1 greater than the main RosJavaLite master.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class AnonymousParmeterTree implements ParameterTree {

  private ParameterTree parameterTree;

  public AnonymousParmeterTree(InetSocketAddress masterUri) throws IOException {
    NodeIdentifier nodeIdentifier = new NodeIdentifier(GraphName.of("invalid"), null);
    parameterTree =
        DefaultParameterTree.newFromNodeIdentifier(nodeIdentifier, masterUri, NameResolver.newRoot(), null);
  }

  @Override
  public Object get(GraphName name, Object defaultObject) {
    return parameterTree.get(name, defaultObject);
  }

  public Object get(GraphName name) { return get(name, false); }
  
  @Override
  public Object get(String name, Object defaultObject) { 
	  return parameterTree.get(name, defaultObject); 
  }
  
  public Object get(String name) { 
	  return parameterTree.get(name, false); 
  }
  
  @Override
  public void set(GraphName name, Object value) {
    parameterTree.set(name, value);
  }

  @Override
  public void set(String name, Object value) {
    parameterTree.set(name, value);
  }

 
  @Override
  public boolean has(GraphName name) {
    return parameterTree.has(name);
  }

  @Override
  public boolean has(String name) {
    return parameterTree.has(name);
  }

  @Override
  public void delete(GraphName name) {
    parameterTree.delete(name);
  }

  @Override
  public void delete(String name) {
    parameterTree.delete(name);
  }

  @Override
  public GraphName search(GraphName name) {
    return parameterTree.search(name);
  }

  @Override
  public GraphName search(String name) {
    return parameterTree.search(name);
  }

  @Override
  public Collection<GraphName> getNames() {
    return parameterTree.getNames();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public void addParameterListener(GraphName name, ParameterListener listener) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public void addParameterListener(String name, ParameterListener listener) {
    throw new UnsupportedOperationException();
  }
}
