package org.ros.node.parameter;

import org.ros.exception.ParameterClassCastException;
import org.ros.exception.ParameterNotFoundException;
import org.ros.internal.node.server.ParameterServer;
import org.ros.namespace.GraphName;

import java.util.Collection;

/**
 * Provides access to a {@link ParameterServer}.
 * 
 * <p>
 * A parameter server is a shared, multi-variate dictionary that is accessible
 * via network APIs. Nodes use this server to store and retrieve parameters at
 * runtime. Its primary intent is to store configuration parameters. It is meant to be
 * globally viewable so that tools can easily inspect the configuration state of
 * the system and modify if necessary.<p/>
 * In RosJavaLite the parameter server can provision remote nodes with JARs and 
 * configuration files to reduce deployment overhead.<p/>
 * 
 * @see <a href="http://www.ros.org/wiki/Parameter%20Server">Parameter server documentation</a>
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public interface ParameterTree {

  /**
   * @param name the parameter name
   * @param defaultValue  the default value
   * @return the parameter value or the default value if the parameter does not exist
   * @throws ParameterClassCastException if the parameter exists and is not the expected type
   */
  Object get(GraphName name, Object defaultValue);

  /**
   * @see #getMap(GraphName, Map)
   */
  Object get(String name, Object defaultValue);

 
  /**
   * @param name the parameter name
   * @param value the value that the parameter will be set to
   */
  void set(GraphName name, Object value);

  /**
   * @see #set(GraphName, Map)
   */
  void set(String name, Object value);

  /**
   * @param name the parameter name
   * @return {@code true} if a parameter with the given name exists, {@code false} otherwise
   */
  boolean has(GraphName name);

  /**
   * @see #has(GraphName)
   */
  boolean has(String name);

  /**
   * Deletes a specified parameter.
   * 
   * @param name the parameter name
   */
  void delete(GraphName name);

  /**
   * @see #delete(GraphName)
   */
  void delete(String name);

  /**
   * Search for parameter key on the Parameter Server. Search starts in caller's
   * namespace and proceeds upwards through parent namespaces until the
   * {@link ParameterServer} finds a matching key.
   * 
   * @param name the parameter name to search for
   * @return the name of the found parameter or {@code null} if no matching parameter was found
   */
  GraphName search(GraphName name);

  /**
   * @see #search(GraphName)
   */
  GraphName search(String name);

  /**
   * @return all known parameter names
   */
  Collection<GraphName> getNames();

  /**
   * Subscribes to changes to the specified parameter.
   * 
   * @param name the parameter name to subscribe to
   * @param listener a {@link ParameterListener} that will be called when the subscribed parameter changes
   */
  void addParameterListener(GraphName name, ParameterListener listener);

  /**
   * @see #addParameterListener(GraphName, ParameterListener)
   */
  void addParameterListener(String name, ParameterListener listener);
}
