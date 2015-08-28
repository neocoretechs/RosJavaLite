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

package org.ros.internal.node.parameter;

import org.ros.exception.ParameterClassCastException;
import org.ros.exception.ParameterNotFoundException;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.client.ParameterClient;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.response.StatusCode;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.ParameterServer;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.namespace.NodeNameResolver;
import org.ros.node.parameter.ParameterListener;
import org.ros.node.parameter.ParameterTree;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Provides access to the ROS {@link ParameterServer}.
 * 
 * @author kwc@willowgarage.com (Ken Conley)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class DefaultParameterTree implements ParameterTree {

  private final ParameterClient parameterClient;
  private final ParameterManager parameterManager;
  private final NameResolver resolver;

  public static DefaultParameterTree newFromNodeIdentifier(NodeIdentifier nodeIdentifier,
    InetSocketAddress inetSocketAddress, NameResolver resolver, ParameterManager parameterManager) throws IOException {
	ParameterClient client = null;
	client = new ParameterClient(nodeIdentifier, inetSocketAddress);
    return new DefaultParameterTree(client, parameterManager, resolver);
  }

  private DefaultParameterTree(ParameterClient parameterClient, ParameterManager parameterManager,
      NameResolver resolver) {
    this.parameterClient = parameterClient;
    this.parameterManager = parameterManager;
    this.resolver = resolver;
  }

  @Override
  public boolean has(GraphName name) {
    GraphName resolvedName = resolver.resolve(name);
    return parameterClient.hasParam(resolvedName).getResult();
  }

  @Override
  public boolean has(String name) {
    return has(GraphName.of(name));
  }

  @Override
  public void delete(GraphName name) {
    GraphName resolvedName = resolver.resolve(name);
    parameterClient.deleteParam(resolvedName);
  }

  @Override
  public void delete(String name) {
    delete(GraphName.of(name));
  }

  @Override
  public GraphName search(GraphName name) {
    GraphName resolvedName = resolver.resolve(name);
    Response<GraphName> response = parameterClient.searchParam(resolvedName);
    if (response.getStatusCode() == StatusCode.SUCCESS) {
      return response.getResult();
    } else {
      return null;
    }
  }

  @Override
  public GraphName search(String name) {
    return search(GraphName.of(name));
  }

  @Override
  public List<GraphName> getNames() {
    return parameterClient.getParamNames().getResult();
  }

  @Override
  public void addParameterListener(GraphName name, ParameterListener listener) {
    parameterManager.addListener(name, listener);
    parameterClient.subscribeParam(name);
  }

  @Override
  public void addParameterListener(String name, ParameterListener listener) {
    addParameterListener(GraphName.of(name), listener);
  }

  @Override
  public void set(GraphName name, Object value) {
    GraphName resolvedName = resolver.resolve(name);
    parameterClient.setParam(resolvedName, value);
  }

  @Override
  public void set(String name, Object value) {
    set(GraphName.of(name), value);
  }

  
  private <T> T getInternal(GraphName name, Class<T> type) {
    GraphName resolvedName = resolver.resolve(name);
    Response<Object> response = parameterClient.getParam(resolvedName);
    try {
      if (response.getStatusCode() == StatusCode.SUCCESS) {
        return type.cast(response.getResult());
      }
    } catch (ClassCastException e) {
      throw new ParameterClassCastException("Cannot cast parameter to: " + type.getName(), e);
    }
    throw new ParameterNotFoundException("Parameter does not exist: " + name);
  }

  @SuppressWarnings("unchecked")
  private <T> T getInternal(GraphName name, T defaultValue) {
    assert(defaultValue != null);
    GraphName resolvedName = resolver.resolve(name);
    Response<Object> response = parameterClient.getParam(resolvedName);
    if (response.getStatusCode() == StatusCode.SUCCESS) {
      try {
        return (T) defaultValue.getClass().cast(response.getResult());
      } catch (ClassCastException e) {
        throw new ParameterClassCastException("Cannot cast parameter to: "
            + defaultValue.getClass().getName(), e);
      }
    } else {
      return defaultValue;
    }
  }


  public static ParameterTree newFromNodeIdentifier(
	NodeIdentifier nodeIdentifier, InetSocketAddress remoteUri,
	NodeNameResolver resolver, ParameterManager parameterManager) throws IOException {
	ParameterClient client = new ParameterClient(nodeIdentifier, remoteUri);
	return new DefaultParameterTree(client, parameterManager, resolver);
  }

@Override
public Object get(GraphName name, Object defaultValue) {
	return getInternal(name, defaultValue);
}

@Override
public Object get(String name, Object defaultValue) {
	return getInternal(GraphName.of(name), defaultValue);
}
/**
 * Retrieve the value and cast to the designated default type
 * @param name
 * @param defaultValue
 * @return
 */
public <T> T get(String name, Class<T> defaultValue) {
	return getInternal(GraphName.of(name), defaultValue);
}
/**
 * Retrieve the value and cast to the designated default type
 * @param name
 * @param defaultValue
 * @return
 */
public <T> T get(GraphName name, Class<T> defaultValue) {
	return getInternal(name, defaultValue);
}
}
