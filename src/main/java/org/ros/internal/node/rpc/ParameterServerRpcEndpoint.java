package org.ros.internal.node.rpc;

import org.ros.internal.node.server.ParameterServer;

import java.util.Collection;
import java.util.List;


/**
 * RPC endpoint for a parameter server.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface ParameterServerRpcEndpoint extends RpcEndpoint {

  /**
   * Deletes a parameter.
   * 
   * @param callerId ROS caller ID
   * @param key parameter name
   * @return void
   */
  public List<Object> deleteParam(String callerId, String key);

  /**
   * Sets a parameter.
   * 
   * <p>
   * NOTE: if value is a dictionary it will be treated as a parameter tree,
   * where key is the parameter namespace. For example
   * {'x':1,'y':2,'sub':{'z':3}} will set key/x=1, key/y=2, and key/sub/z=3.
   * Furthermore, it will replace all existing parameters in the key parameter
   * namespace with the parameters in value. You must set parameters
   * individually if you wish to perform a union update.
   * 
   * @param callerId
   *          ROS caller ID
   * @param key
   *          Parameter name.
   * @param value
   *          Parameter value.
   * @return void
   */
 
  public List<Object> setParam(String callerId, String key, Object value);

  /**
   * Retrieve parameter value from server.
   * 
   * <p>
   * If code is not 1, parameterValue should be ignored. If key is a namespace,
   * the return value will be a dictionary, where each key is a parameter in
   * that namespace. Sub-namespaces are also represented as dictionaries.
   * 
   * @param callerId ROS caller ID
   * @param key Parameter name. If key is a namespace, getParam() will return a parameter tree.
   * @return the parameter value
   */
  public List<Object> getParam(String callerId, String key);

  /**
   * Searches for a parameter key on the {@link ParameterServer}.
   * 
   * <p>
   * Search starts in caller's namespace and proceeds upwards through parent
   * namespaces until Parameter Server finds a matching key. searchParam()'s
   * behavior is to search for the first partial match. For example, imagine
   * that there are two 'robot_description' parameters /robot_description
   * /robot_description/arm /robot_description/base /pr2/robot_description
   * /pr2/robot_description/base If I start in the namespace /pr2/foo and search
   * for robot_description, searchParam() will match /pr2/robot_description. If
   * I search for robot_description/arm it will return
   * /pr2/robot_description/arm, even though that parameter does not exist
   * (yet).
   * 
   * If code is not 1, foundKey should be ignored.
   * 
   * @param callerId
   *          ROS caller ID
   * @param key
   *          Parameter name to search for.
   * @return the found key
   */
  public List<Object> searchParam(String callerId, String key);

  /**
   * Retrieves the parameter value from server and subscribe to updates to that
   * param. See paramUpdate() in the Node API.
   * 
   * <p>
   * If code is not 1, parameterValue should be ignored. parameterValue is an
   * empty dictionary if the parameter has not been set yet.
   * 
   * @param callerId  ROS caller ID
   * @param callerApi Node API URI of subscriber for paramUpdate callbacks.
   * @param key
   * @return the parameter value
   */
  public List<Object> subscribeParam(String callerId, String callerApi, String callerPort, String key);

  /**
   * Unsubscribes from updates to the specified param. See paramUpdate() in the
   * Node API.
   * 
   * <p>
   * A return value of zero means that the caller was not subscribed to the
   * parameter.
   * 
   * @param callerId ROS caller ID
   * @param callerApi Node API URI of subscriber
   * @param key Parameter name
   * @return the number of parameters that were unsubscribed
   */
  public List<Object> unsubscribeParam(String callerId, String callerApi, String callerPort, String key);

  /**
   * Check if parameter is stored on server.
   * 
   * @param callerId ROS caller ID.
   * @param key Parameter name.
   * @return {@code true} if the parameter exists
   */
  public List<Object> hasParam(String callerId, String key);

  /**
   * Gets the list of all parameter names stored on this server.
   * 
   * @param callerId ROS caller ID.
   * @return a {@link Collection} of parameter names
   */
  public List<Object> getParamNames(String callerId);

}